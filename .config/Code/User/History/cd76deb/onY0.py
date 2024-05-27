import sys
import gitlab
import logging
from os import getenv

# Configurar logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

def initialize_gitlab(url, token):
    try:
        gl = gitlab.Gitlab(url, private_token=token)
        gl.auth()
        logging.info("Successfully authenticated to GitLab.")
        return gl
    except Exception as e:
        logging.error(f'Error initializing GitLab: {e}')
        sys.exit(1)

def check_or_create_namespace(gl, namespace_name):
    try:
        group = gl.groups.get(namespace_name)
        logging.info(f'Namespace "{namespace_name}" already exists.')
        return group
    except gitlab.exceptions.GitlabGetError:
        group = gl.groups.create({'name': namespace_name, 'path': namespace_name})
        logging.info(f'Namespace "{namespace_name}" created.')
        return group

def check_or_create_project(gl, namespace, project_name):
    full_path = f"{namespace.full_path}/{project_name}"
    try:
        project = gl.projects.get(full_path)
        logging.info(f'Project "{full_path}" already exists.')
        return project
    except gitlab.exceptions.GitlabGetError:
        try:
            project = gl.projects.create({'name': project_name, 'namespace_id': namespace.id})
            logging.info(f'Project "{project_name}" created in namespace "{namespace.name}".')
            return project
        except gitlab.exceptions.GitlabCreateError as e:
            logging.error(f'Failed to create project "{project_name}": {e.error_message}')
            sys.exit(1)

if __name__ == '__main__':
    if len(sys.argv) != 5:
        logging.error("Usage: python gitlab_management.py <gitlab_url> <private_token> <namespace_name> <project_name>")
        sys.exit(1)

    gitlab_url = sys.argv[1]
    private_token = sys.argv[2]
    namespace_name = sys.argv[3]
    project_name = sys.argv[4]

    gl = initialize_gitlab(gitlab_url, private_token)
    namespace = check_or_create_namespace(gl, namespace_name)
    project = check_or_create_project(gl, namespace, project_name)

    logging.info(f'{{"namespace_id": {namespace.id}, "project_id": {project.id}}}')
