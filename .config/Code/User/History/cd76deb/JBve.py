import sys
import gitlab
import logging

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

def find_or_create_group(gl, path):
    elements = path.split('/')
    project_name = elements[-1]  # El último elemento es el nombre del proyecto
    parent_id = None

    # Excluyendo el último elemento ya que es el nombre del proyecto
    for element in elements[:-1]:  
        found_group = False
        try:
            groups = gl.groups.list(search=element)
            for group in groups:
                if (group.parent_id == parent_id or (group.parent_id is None and parent_id is None)) and group.path == element:
                    found_group = True
                    break
            if not found_group:
                raise gitlab.exceptions.GitlabGetError("Group not found")
        except gitlab.exceptions.GitlabGetError:
            group_data = {'name': element, 'path': element}
            if parent_id:
                group_data['parent_id'] = parent_id
            group = gl.groups.create(group_data)
            logging.info(f'Group "{element}" created.')
        else:
            if found_group:
                group = group
                logging.info(f'Group "{element}" already exists.')
        parent_id = group.id  # Actualizar parent_id para anidar el siguiente grupo si es necesario

    return parent_id, project_name  # Devuelve el ID del último grupo y el nombre del proyecto

def create_project_in_group(gl, parent_id, project_name):
    try:
        project = gl.projects.get(f"{gl.groups.get(parent_id).full_path}/{project_name}")
        logging.info(f'Project "{project_name}" already exists in group with ID {parent_id}.')
    except gitlab.exceptions.GitlabGetError:
        try:
            project = gl.projects.create({'name': project_name, 'namespace_id': parent_id})
            logging.info(f'Project "{project_name}" created in group with ID {parent_id}.')
        except gitlab.exceptions.GitlabCreateError as e:
            logging.error(f'Failed to create project "{project_name}": {e.error_message}')
            sys.exit(1)
    return project

if __name__ == '__main__':
    if len(sys.argv) != 4:
        logging.error("Usage: python gitlab_management.py <gitlab_url> <private_token> <full_path>")
        sys.exit(1)

    gitlab_url = sys.argv[1]
    private_token = sys.argv[2]
    full_path = sys.argv[3]

    gl = initialize_gitlab(gitlab_url, private_token)
    parent_id, project_name = find_or_create_group(gl, full_path)
    project = create_project_in_group(gl, parent_id, project_name)

    logging.info(f'{{"group_id": {parent_id}, "project_id": {project.id}}}')
