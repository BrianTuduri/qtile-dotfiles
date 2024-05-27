def urlA = "https://gitlab.geocom.com.uy/uy-com-geocom-scm/devops/snippets-in-vscode.git"
def urlB = "git@gitlab.geocom.com.uy:uy-com-geocom-scm/devops/snippets-in-vscode.git"

def projectName = urlA.replaceFirst(/\.git$/, "").tokenize('/').last()
def gitlabServer = urlA.split('/')
println gitlabServer