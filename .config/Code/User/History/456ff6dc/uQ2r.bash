#!/usr/bin/env bash

set -ex
REPO_HOME=$(dirname "$(pwd)") && \
echo "Running playbook at $REPO_HOME"
cd ansible
ansible-galaxy install -r ansible/requirements.yml && \
ansible-playbook ansible/playbooks/main.yml \
-i ansible/inventory.ini \
-e '@ansible/ansible.vars.yml' \
-e 'repo_home=${pwd()}'\
-e "ansible_user=root" \
-e 'ambiente=${params.ambiente}' \
-t java
