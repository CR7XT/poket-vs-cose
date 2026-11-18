package com.example.data.git

import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.treewalk.filter.PathFilter

enum class GitChangeType(val badge: String) {
  ADDED("A"),
  MODIFIED("M"),
  DELETED("D"),
  UNTRACKED("U"),
}

data class GitFileChange(
  val path: String,
  val type: GitChangeType,
  val staged: Boolean,
)

data class GitCommitSummary(
  val shortId: String,
  val message: String,
  val author: String,
  val formattedDate: String,
)

data class GitStatusSnapshot(
  val isRepo: Boolean = false,
  val branchName: String = "no-git",
  val staged: List<GitFileChange> = emptyList(),
  val unstaged: List<GitFileChange> = emptyList(),
  val commits: List<GitCommitSummary> = emptyList(),
)

enum class DiffLineType {
  HEADER,
  HUNK,
  ADD,
  DELETE,
  CONTEXT,
}

data class GitDiffLine(
  val type: DiffLineType,
  val oldLine: Int? = null,
  val newLine: Int? = null,
  val text: String,
)

class GitRepository(private val workspaceRoot: File) {

  suspend fun inspectStatus(): GitStatusSnapshot =
    withContext(Dispatchers.IO) {
      val gitDir = File(workspaceRoot, ".git")
      if (!gitDir.exists()) {
        return@withContext GitStatusSnapshot(isRepo = false, branchName = "no-git")
      }
      Git.open(workspaceRoot).use { git ->
        val branch = runCatching { git.repository.branch ?: "HEAD" }.getOrDefault("HEAD")
        val status = git.status().call()

        val staged = buildList {
          status.added.sorted().forEach { add(GitFileChange(it, GitChangeType.ADDED, true)) }
          status.changed.sorted().forEach { add(GitFileChange(it, GitChangeType.MODIFIED, true)) }
          status.removed.sorted().forEach { add(GitFileChange(it, GitChangeType.DELETED, true)) }
        }

        val unstaged = buildList {
          status.modified.sorted().forEach { add(GitFileChange(it, GitChangeType.MODIFIED, false)) }
          status.missing.sorted().forEach { add(GitFileChange(it, GitChangeType.DELETED, false)) }
          status.untracked.sorted().forEach { add(GitFileChange(it, GitChangeType.UNTRACKED, false)) }
        }

        val formatter = SimpleDateFormat("MMM d, HH:mm", Locale.US)
        val commits =
          runCatching {
            git.log().setMaxCount(25).call().map { commit ->
              val date = Date(commit.commitTime.toLong() * 1000L)
              GitCommitSummary(
                shortId = commit.id.abbreviate(7).name(),
                message = commit.shortMessage,
                author = commit.authorIdent.name,
                formattedDate = formatter.format(date),
              )
            }
          }.getOrDefault(emptyList())

        GitStatusSnapshot(
          isRepo = true,
          branchName = branch,
          staged = staged,
          unstaged = unstaged,
          commits = commits,
        )
      }
    }

  suspend fun initRepository(): GitStatusSnapshot =
    withContext(Dispatchers.IO) {
      Git.init().setDirectory(workspaceRoot).setInitialBranch("main").call().close()
      inspectStatus()
    }

  suspend fun cloneRepository(url: String, token: String?): GitStatusSnapshot =
    withContext(Dispatchers.IO) {
      val cleanUrl = url.trim()
      require(cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://")) {
        "Repository URL must start with https://"
      }
      // Clone into clean workspace or subfolder named after repo
      val repoName =
        cleanUrl.substringAfterLast('/').removeSuffix(".git").ifBlank { "cloned-repo" }
      val targetDir =
        if (workspaceRoot.listFiles()?.isEmpty() != false) workspaceRoot
        else File(workspaceRoot, repoName)
      if (targetDir != workspaceRoot && targetDir.exists()) {
        throw IllegalStateException("Folder '$repoName' already exists in workspace")
      }
      val cmd = Git.cloneRepository().setURI(cleanUrl).setDirectory(targetDir)
      if (!token.isNullOrBlank()) {
        cmd.setCredentialsProvider(UsernamePasswordCredentialsProvider("oauth2", token.trim()))
      }
      cmd.call().close()
      inspectStatus()
    }

  suspend fun stageFile(path: String): GitStatusSnapshot =
    withContext(Dispatchers.IO) {
      Git.open(workspaceRoot).use { git ->
        val file = File(workspaceRoot, path)
        if (file.exists()) {
          git.add().addFilepattern(path).call()
        } else {
          git.rm().addFilepattern(path).call()
        }
      }
      inspectStatus()
    }

  suspend fun stageAll(): GitStatusSnapshot =
    withContext(Dispatchers.IO) {
      Git.open(workspaceRoot).use { git ->
        git.add().addFilepattern(".").call()
        git.add().setUpdate(true).addFilepattern(".").call()
      }
      inspectStatus()
    }

  suspend fun unstageFile(path: String): GitStatusSnapshot =
    withContext(Dispatchers.IO) {
      Git.open(workspaceRoot).use { git ->
        val headId = git.repository.resolve("HEAD")
        if (headId != null) {
          git.reset().addPath(path).call()
        } else {
          git.rm().setCached(true).addFilepattern(path).call()
        }
      }
      inspectStatus()
    }

  suspend fun commit(message: String, authorName: String, authorEmail: String): GitStatusSnapshot =
    withContext(Dispatchers.IO) {
      val cleanMessage = message.trim()
      require(cleanMessage.isNotEmpty()) { "Commit message cannot be empty" }
      val name = authorName.trim().ifEmpty { "PocketCode User" }
      val email = authorEmail.trim().ifEmpty { "developer@pocketcode.local" }
      Git.open(workspaceRoot).use { git ->
        git.commit()
          .setMessage(cleanMessage)
          .setAuthor(name, email)
          .setCommitter(name, email)
          .call()
      }
      inspectStatus()
    }

  suspend fun computeDiff(change: GitFileChange): List<GitDiffLine> =
    withContext(Dispatchers.IO) {
      if (change.type == GitChangeType.UNTRACKED) {
        val file = File(workspaceRoot, change.path)
        if (!file.exists()) return@withContext emptyList()
        val lines = runCatching { file.readLines(Charsets.UTF_8) }.getOrDefault(emptyList())
        return@withContext buildList {
          add(GitDiffLine(DiffLineType.HEADER, text = "--- /dev/null"))
          add(GitDiffLine(DiffLineType.HEADER, text = "+++ b/${change.path}"))
          add(GitDiffLine(DiffLineType.HUNK, text = "@@ -0,0 +1,${lines.size} @@"))
          lines.forEachIndexed { idx, line ->
            add(GitDiffLine(DiffLineType.ADD, oldLine = null, newLine = idx + 1, text = line))
          }
        }
      }

      Git.open(workspaceRoot).use { git ->
        val repo = git.repository
        val out = ByteArrayOutputStream()
        DiffFormatter(out).use { formatter ->
          formatter.setRepository(repo)
          formatter.setContext(3)
          formatter.setPathFilter(PathFilter.create(change.path))

          val oldTree =
            if (change.staged) {
              val headId: ObjectId? = repo.resolve("HEAD^{tree}")
              if (headId == null) {
                EmptyTreeIterator()
              } else {
                RevWalk(repo).use { walk ->
                  val tree = walk.parseTree(headId)
                  val reader = repo.newObjectReader()
                  CanonicalTreeParser().apply { reset(reader, tree) }
                }
              }
            } else {
              DirCacheIterator(repo.readDirCache())
            }

          val newTree =
            if (change.staged) {
              DirCacheIterator(repo.readDirCache())
            } else {
              FileTreeIterator(repo)
            }

          val entries = formatter.scan(oldTree, newTree)
          formatter.format(entries)
        }
        parseUnifiedDiff(out.toString(Charsets.UTF_8.name()))
      }
    }

  private fun parseUnifiedDiff(raw: String): List<GitDiffLine> {
    if (raw.isBlank()) return listOf(GitDiffLine(DiffLineType.HEADER, text = "No textual diff available"))
    val lines = raw.lines()
    val result = mutableListOf<GitDiffLine>()
    var oldLineNum = 0
    var newLineNum = 0
    val hunkRegex = Regex("""^@@ -(\d+)(?:,\d+)? \+(\d+)(?:,\d+)? @@.*""")

    for (line in lines) {
      when {
        line.startsWith("diff ") ||
          line.startsWith("index ") ||
          line.startsWith("--- ") ||
          line.startsWith("+++ ") -> {
          result += GitDiffLine(DiffLineType.HEADER, text = line)
        }
        line.startsWith("@@") -> {
          val match = hunkRegex.matchEntire(line)
          if (match != null) {
            oldLineNum = match.groupValues[1].toIntOrNull() ?: 1
            newLineNum = match.groupValues[2].toIntOrNull() ?: 1
          }
          result += GitDiffLine(DiffLineType.HUNK, text = line)
        }
        line.startsWith("+") -> {
          result +=
            GitDiffLine(
              DiffLineType.ADD,
              oldLine = null,
              newLine = newLineNum,
              text = line.drop(1),
            )
          newLineNum++
        }
        line.startsWith("-") -> {
          result +=
            GitDiffLine(
              DiffLineType.DELETE,
              oldLine = oldLineNum,
              newLine = null,
              text = line.drop(1),
            )
          oldLineNum++
        }
        line.startsWith(" ") -> {
          result +=
            GitDiffLine(
              DiffLineType.CONTEXT,
              oldLine = oldLineNum,
              newLine = newLineNum,
              text = line.drop(1),
            )
          oldLineNum++
          newLineNum++
        }
      }
    }
    return result
  }
}
