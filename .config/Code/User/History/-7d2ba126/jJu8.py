import requests
import csv

gitlab_url = 'https://gitnuevo.geocom.com.uy'
private_token = 'TOKEN'
headers = {'PRIVATE-TOKEN': private_token}

# Obtener la lista de grupos

groups_data = []
page=1
for page in range(50):
    response = requests.get(f'{gitlab_url}/api/v4/groups', params={'page': page},headers=headers)
    groups_data += response.json()
    if groups_data:
        break

# Crear un archivo CSV
with open('reporte_permisos.csv', 'w', newline='') as csvfile:
    csv_writer = csv.writer(csvfile)
    csv_writer.writerow(['Grupo', 'Proyecto', 'Miembro'])

    for group in groups_data:
        
        group_id = group['id']
        group_name = group['full_path']

        print(f'{group_name}')
        projects_response = requests.get(f'{gitlab_url}/api/v4/groups/{group_id}/projects', headers=headers)
        projects_data = projects_response.json()

        for project in projects_data:
            project_id = project['id']
            project_name = project['name']

            members_response = requests.get(f'{gitlab_url}/api/v4/projects/{project_id}/members', headers=headers)
            members_data = members_response.json()

            for member in members_data:
                member_username = member['username']
                csv_writer.writerow([group_name, project_name, member_username])

print("Informe de permisos generado en reporte_permisos.csv")
