import sys
import gitlab

def initialize_gitlab(url, token):
    try:
        gl = gitlab.Gitlab(url, private_token=token)
        gl.auth()
        return gl
    except Exception as e:
        print(f'Error initializing GitLab: {e}')
        sys.exit(1)

def check_or_create_namespace(gl, namespace_name):
    try:
        # Intentar obtener el grupo
        group = gl.groups.get(namespace_name)
    except gitlab.exceptions.GitlabGetError:
        # Si no existe, crear el grupo
        group = gl.groups.create({'name': namespace_name, 'path': namespace_name})
        print(f'Namespace "{namespace_name}" created.')
    return group.id

def check_or_create_project(gl, namespace_id, project_name):
    try:
        # Intentar obtener el proyecto
        project = gl.projects.get(f'{namespace_id}/{project_name}')
    except gitlab.exceptions.GitlabGetError:
        # Si no existe, crear el proyecto
        project = gl.projects.create({'name': project_name, 'namespace_id': namespace_id})
        print(f'Project "{project_name}" created.')
    return project.id

if __name__ == '__main__':
    if len(sys.argv) != 5:
        print("Usage: python gitlab_management.py <gitlab_url> <private_token> <namespace_name> <project_name>")
        sys.exit(1)

    gitlab_url = sys.argv[1]
    private_token = sys.argv[2]
    namespace_name = sys.argv[3]
    project_name = sys.argv[4]

    gl = initialize_gitlab(gitlab_url, private_token)
    namespace_id = check_or_create_namespace(gl, namespace_name)
    project_id = check_or_create_project(gl, namespace_id, project_name)

    print(f'{{"namespace_id": {namespace_id}, "project_id": {project_id}}}')