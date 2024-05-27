def repo_url = "https://gitlab.geocom.com.uy/uy-com-geocom-scm/devops/snippets-in-vscode.git"

def projectName = urlB.replaceFirst(/\.git$/, "").tokenize('/').last()
def gitlabServerSSH = urlB.split('@')[1].toString().split(':')[0]
def gitlabServerHTTPS = repo_url.split('/')[2]

def gitlabU = url.contains('git@') ? repo_url.split('@')[1].toString().split(':')[0] : url.split('/')[2]

println gitlabU
//println gitlabServerHTTPS