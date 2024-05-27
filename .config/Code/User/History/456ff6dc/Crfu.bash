#!/usr/bin/env bash

set -ex
REPO_HOME=$(dirname "$(pwd)") && \
echo "Running playbook at $REPO_HOME"
ansible-galaxy install -r ansible/requirements.yml && \
ansible-playbook ansible/playbooks/main.yml \
-i ansible/inventory.ini \
-e "ansible_user=root" \
-e '@ansible/ansible.vars.yml' \
-e "repo_home=$REPO_HOME"\
-e 'ambiente=QA' \
-t java
