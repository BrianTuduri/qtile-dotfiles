def urlA = "https://gitlab.geocom.com.uy/uy-com-geocom-scm/devops/snippets-in-vscode.git"
def urlB = "git@gitlab.geocom.com.uy:uy-com-geocom-scm/devops/snippets-in-vscode.git"

def baseRepoUrl = urlA.replaceFirst(/\.git$/, "")
def projectName = baseRepoUrl.tokenize('/').last()

println baseRepoUrl