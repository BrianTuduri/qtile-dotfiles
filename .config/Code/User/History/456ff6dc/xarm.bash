#!/usr/bin/env bash

set -ex
REPO_HOME=$(dirname "$(pwd)") && \
echo "Running playbook at $REPO_HOME"
cd ansible
ansible-galaxy install -r requirements.yml -vvvv
ansible-playbook \
    playbooks/install_kafka.yml \
    -i inventory.ini \
    -e "ansible_user=root" \
    -e @ansible.vars.yml --tags "java"
