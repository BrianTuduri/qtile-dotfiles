def urlA = "https://gitlab.geocom.com.uy/uy-com-geocom-scm/devops/snippets-in-vscode.git"
def urlB = "git@gitlab.geocom.com.uy:uy-com-geocom-scm/devops/snippets-in-vscode.git"

def projectName = urlB.replaceFirst(/\.git$/, "").tokenize('/').last()
def gitlabServerSSH = urlB.split('@')[1].toString().split(':')[0]
def gitlabServerHTTPS = urlA.split('/')[2]

println projectName
println gitlabServerHTTPS