import gitlab
import sys
import argparse

def gitlab_connection():
    try:
        gl = gitlab.Gitlab('https://gitlab.geocom.com.uy', private_token='TOKEN')
        gl.auth()
        return gl
    except Exception as e:
        print(f"Error al conectar con GitLab: {e}")
        return None

# Función para buscar el ID de un namespace por su nombre
def get_namespace_id_by_name(namespace_name):
    gl = gitlab_connection()
    if gl:
        try:
            namespaces = gl.namespaces.list(search=namespace_name)
            if namespaces:
                return namespaces[0].id
            else:
                return None
        except Exception as e:
            print(f"Error al buscar el namespace: {e}")
            return None

# Función para verificar si un proyecto existe por su nombre
def project_exists(project_name):
    gl = gitlab_connection()
    if gl:
        try:
            project = gl.projects.get(project_name)
            if project:
                return True
            else:
                return False
        except Exception as e:
            return False

# Obtener el ID del namespace por su nombre
def get_namespace_id_by_name_arg(namespace_name):
    namespace_id = get_namespace_id_by_name(namespace_name)
    return namespace_id

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