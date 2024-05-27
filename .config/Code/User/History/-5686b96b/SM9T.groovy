def repo_url = "git@gitlab.geocom.com.uy:uy-com-geocom-scm/devops/snippets-in-vscode.git"

//def projectName = repo_url.replaceFirst(/\.git$/, "").tokenize('/').last()
//def gitlabServerSSH = repo_url.split('@')[1].toString().split(':')[0]
//def gitlabServerHTTPS = repo_url.split('/')[2]

if (repo_url.contains('git@')) {
    println("El repositorio es SSH")
} else { println("El repositorio es HTTPS") }

def gitlabU = repo_url.contains('git@') ? repo_url.split('@')[1].toString().split(':')[0] : repo_url.split('/')[2]

println gitlabU
//println gitlabServerHTTPS