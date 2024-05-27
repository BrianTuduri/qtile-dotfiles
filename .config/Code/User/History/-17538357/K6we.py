import subprocess
import os

# Leer las variables de entorno
REPO_PATH = os.getenv('REPO_PATH', '/path/to/repos')
SELECTOR = os.getenv('SELECTOR', 'rofi')  # Puede ser 'wofi' o 'rofi'
EDITOR = os.getenv('EDITOR', 'code')  # Puede ser 'code', 'codium', 'intellij', etc.

try:
    # Comando para listar las rutas completas de los directorios en el path especificado
    command = f"find '{REPO_PATH}' -maxdepth 1 -type d -print"
    repo_paths = subprocess.check_output(command, shell=True, text=True).strip().split('\n')
    # Eliminar el propio REPO_PATH del listado
    repo_paths = [path for path in repo_paths if path != REPO_PATH]
    # Obtener solo los nombres de los directorios para mostrar en el selector
    repo_names = [os.path.basename(path) for path in repo_paths]
except subprocess.CalledProcessError as e:
    print(f"Error al listar los repositorios: {e}")
    exit(1)

# Preparar la lista de nombres de repositorios como entrada para wofi/rofi
input_str = "\n".join(repo_names)

# Ejecutar wofi/rofi y capturar la elección
try:
    selected_repo_name = subprocess.run([SELECTOR, '-dmenu'], input=input_str, text=True, capture_output=True, check=True).stdout.strip()
    # Obtener la ruta completa del repositorio seleccionado
    selected_repo_path = next((path for path, name in zip(repo_paths, repo_names) if name == selected_repo_name), None)
except subprocess.CalledProcessError as e:
    print(f"Error al seleccionar el repositorio: {e}")
    exit(1)

# Abrir el repositorio seleccionado con el editor especificado
if selected_repo_path:
    try:
        subprocess.Popen([EDITOR, selected_repo_path])
    except Exception as e:
        print(f"Error al abrir el repositorio con el editor: {e}")
else:
    print("No se seleccionó ningún repositorio.")