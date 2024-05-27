import gitlab
import argparse
from gitlab.exceptions import GitlabGetError

# Configura la conexión con GitLab
def gitlab_connection():
    try:
        gl = gitlab.Gitlab('https://gitlab.geocom.com.uy', private_token='TOKEN')
        gl.auth()
        return gl
    except Exception as e:
        print(f"Error al conectar con GitLab: {e}")
        return None
# Función para buscar el ID de un namespace por su nombre y devolver True o False
def get_namespace_id_by_name(namespace_name):
    gl = gitlab_connection()
    if gl:
        try:
            namespaces = gl.namespaces.list(search=namespace_name)
            if namespaces:
                return True
            else:
                return False
        except Exception as e:
            print(f"Error al buscar el namespace: {e}")
            return False

# Función para verificar si un proyecto existe por su nombre y devolver True o False
def project_exists(project_name):
    gl = gitlab_connection()
    if gl:
        try:
            gl.projects.get(project_name)  # Intenta obtener el proyecto
            return True
        except GitlabGetError as e:
            if e.response_code == 404:
                return False  # El proyecto no se encontró, devuelve False
            else:
                print(f"Error al verificar si el proyecto existe: {e}")
                return False

# Obtener el ID del namespace por su nombre y devolver True o False
def get_namespace_id_by_name_arg(namespace_name):
    return get_namespace_id_by_name(namespace_name)

# Ejemplo de uso
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Verificar la existencia de proyectos y namespaces en GitLab")
    parser.add_argument("-P", "--project", metavar="PROJECT_NAME", help="Verificar si existe un proyecto por su nombre")
    parser.add_argument("-N", "--namespace", metavar="NAMESPACE_NAME", help="Verificar si existe un namespace por su nombre")
    parser.add_argument("-NN", "--namespace_name", metavar="NAMESPACE_NAME", help="Obtener el nombre del namespace por su ID")

    args = parser.parse_args()

    if args.project:
        result = project_exists(args.project)
        print(result)

    if args.namespace:
        result = get_namespace_id_by_name(args.namespace)
        print(result)

    if args.namespace_name:
        result = get_namespace_id_by_name_arg(args.namespace_name)
        print(result)

# Ejemplo de uso
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Verificar la existencia de proyectos y namespaces en GitLab y crearlos si no existen")
    parser.add_argument("project_name", metavar="PROJECT_NAME", help="Nombre del proyecto")
    parser.add_argument("namespace_name", metavar="NAMESPACE_NAME", help="Nombre del namespace")

    args = parser.parse_args()

    if project_exists(args.project_name, args.namespace_name):
        print(f"El proyecto '{args.project_name}' existe en GitLab.")
    else:
        # Si el proyecto no existe, intenta crearlo en el namespace especificado
        if create_project(args.project_name, args.namespace_name):
            print(f"El proyecto '{args.project_name}' ha sido creado en GitLab.")