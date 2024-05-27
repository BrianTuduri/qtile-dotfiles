import subprocess
import os

# envs
REPO_PATH = os.getenv('REPO_PATH', '/path/to/repos')
SELECTOR = os.getenv('SELECTOR', 'rofi')  # 'wofi', 'rofi'
EDITOR = os.getenv('EDITOR', 'code')  # 'code', 'codium', 'intellij', etc.

try:
    # list repos
    command = f"find '{REPO_PATH}' -maxdepth 1 -type d -print"
    repo_paths = subprocess.check_output(command, shell=True, text=True).strip().split('\n')
    repo_paths = [path for path in repo_paths if path != REPO_PATH]
    repo_names = [os.path.basename(path) for path in repo_paths]
except subprocess.CalledProcessError as e:
    print(f"Error al listar los repositorios: {e}")
    exit(1)

input_str = "\n".join(repo_names)

try:
    selected_repo_name = subprocess.run([SELECTOR, '-dmenu'], input=input_str, text=True, capture_output=True, check=True).stdout.strip()
    selected_repo_path = next((path for path, name in zip(repo_paths, repo_names) if name == selected_repo_name), None)
except subprocess.CalledProcessError as e:
    print(f"Error al seleccionar el repositorio: {e}")
    exit(1)

if selected_repo_path:
    try:
        subprocess.Popen([EDITOR, selected_repo_path])
    except Exception as e:
        print(f"Error al abrir el repositorio con el editor: {e}")
else:
    print("No se seleccionó ningún repositorio.")