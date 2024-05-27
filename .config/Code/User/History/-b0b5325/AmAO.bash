#!/bin/env bash
set -xe
cd "$(dirname "$0")" # Move to script location
[ -f "."envrc ] && source .envrc || echo ".envrc does not exist. Ensure you are loading your envs";

REPO_HOME="$(pwd)"
export ANSIBLE_ROLES_PATH="$REPO_HOME/roles" # Define Ansible roles path relative to the repository home.
export ANSIBLE_STDOUT_CALLBACK="yaml" # Define Ansible roles path relative to the repository home.

# ansible-playbook \
#     "$REPO_HOME/roles/keepalived/playbooks/LinuxCluster.yml" \
#         -i "$TEMP_INVENTORY"

ansible-galaxy install -r ansible/requirements.yml
ansible-playbook \
    "ansible/playbook.yml"  \
        -i "ansible/inventory.ini" \
        -e "vault_token=$VAULT_TOKEN"