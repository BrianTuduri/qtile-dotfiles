#!/bin/env bash
set -xe
cd "$(dirname "$0")" # Move to script location
[ -f "."envrc ] && source .envrc || echo ".envrc does not exist. Ensure you are loading your envs";

TEMP_INVENTORY=$(mktemp)
trap "rm -f $TEMP_INVENTORY" EXIT # Delete it when script is finished
cat > "$TEMP_INVENTORY" <<EOF
# Auto-generated inventory file
[masters]
172.24.26.[34:36]
[workers]
172.24.26.[66:68] storage=true
172.24.26.[69:70]

[all:vars]
# Keepalived
keepalived_cni="27"
keepalived_vip="172.24.26.40"
keepalived_interface="ens192"

# Storage point
partition_fstype=ext4
longhorn_mount_source=/dev/sdb
longhorn_mount_point=/mnt/longhorn

# State backend config
backend_address="https://gitlab.geocom.com.uy/api/v4/projects/2951"
backend_username="$BACKEND_USERNAME"
backend_password="$BACKEND_TOKEN"

# Rancher
rancher_url="https://rancher.geocom.com.uy"
rancher_token_key="$RANCHER_TOKEN"
service_account_name="geoscm"

# Vault
vault_addr="https://vault-server.geocom.com.uy"
vault_token="$VAULT_TOKEN"

# LDAP
ldapUser="gitlabsp@geocom.com.uy"
ldapPass="3[6}L'&z^B%sg?&#)W"

# Cluster config
cluster_name="qa"
global_registry="nexus-mirror.geocom.com.uy"
# Your ingress
metallb_default_ip="172.24.26.80"

EOF

export ANSIBLE_ROLES_PATH="$REPO_HOME/roles" # Define Ansible roles path relative to the repository home.
export ANSIBLE_STDOUT_CALLBACK="yaml" # Define Ansible roles path relative to the repository home.

# ansible-playbook \
#     "$REPO_HOME/roles/keepalived/playbooks/LinuxCluster.yml" \
#         -i "$TEMP_INVENTORY"

terraform fmt -recursive
ansible-galaxy collection install kubernetes.core
ansible-playbook \
    "Cluster.yml"  \
        -i "$TEMP_INVENTORY" \
        -t "configuration, vault" \
        --skip-tags "keepalived";