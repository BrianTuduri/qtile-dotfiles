def urlA = "https://gitlab.geocom.com.uy/uy-com-geocom-scm/devops/snippets-in-vscode.git"
def urlB = "git@gitlab.geocom.com.uy:uy-com-geocom-scm/devops/snippets-in-vscode.git"

def projectName = urlB.replaceFirst(/\.git$/, "").tokenize('/').last()
def gitlabServer = urlB.replaceFirst(/\.git$/, "").split('/')[2]

println projectName
println gitlabServer