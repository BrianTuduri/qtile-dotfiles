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
    parent_id = None

    for element in elements[:-1]:  # Exclude the last element since it's the project
        try:
            if parent_id:
                group = next(g for g in gl.groups.list(search=element) if g.parent_id == parent_id)
            else:
                group = gl.groups.get(element)
        except StopIteration:
            group_data = {'name': element, 'path': element}
            if parent_id:
                group_data['parent_id'] = parent_id
            group = gl.groups.create(group_data)
            logging.info(f'Group "{element}" created.')
        except gitlab.exceptions.GitlabGetError:
            group = gl.groups.create({'name': element, 'path': element, 'parent_id': parent_id})
            logging.info(f'Group "{element}" created.')

        parent_id = group.id  # Update parent_id to nest next group if necessary

    return parent_id  # Return the ID of the last group

def create_project_in_group(gl, parent_id, project_name):
    try:
        project = gl.projects.create({'name': project_name, 'namespace_id': parent_id})
        logging.info(f'Project "{project_name}" created in group with ID {parent_id}.')
        return project
    except gitlab.exceptions.GitlabCreateError as e:
        logging.error(f'Failed to create project "{project_name}": {e.error_message}')
        sys.exit(1)

if __name__ == '__main__':
    if len(sys.argv) != 5:
        logging.error("Usage: python gitlab_management.py <gitlab_url> <private_token> <full_path> <project_name>")
        sys.exit(1)

    gitlab_url = sys.argv[1]
    private_token = sys.argv[2]
    full_path = sys.argv[3]
    project_name = sys.argv[4]

    gl = initialize_gitlab(gitlab_url, private_token)
    parent_id = find_or_create_group(gl, full_path)
    project = create_project_in_group(gl, parent_id, project_name)

    logging.info(f'{{"group_id": {parent_id}, "project_id": {project.id}}}')
