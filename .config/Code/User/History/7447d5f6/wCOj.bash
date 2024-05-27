#!/bin/env bash

set -ex

CLUSTER="qa"
PROJECT_ID="2951"
export \
    TF_USERNAME="rdifede" \
    TF_PASSWORD="glpat-4KHG1M9rxze9k7yz58Va" \
    TF_STATE_ADDRESS="https://gitlab.com/api/v4/projects/${PROJECT_ID}/terraform/state/${CLUSTER}" \
    KUBECONFIG_PATH=/home/brian/Downloads/qa.yaml \
    SPRING_LDAP_USERNAME="gitlabsp@geocom.com.uy" \

export SPRING_LDAP_PASSWORD="3[6}L'&z^B%sg?&#)W"

rm -rf /home/brian/repos-geocom/kafka-deploy-oxxo/terraform/.terraform/modules

terragrunt init -reconfigure -upgrade
terragrunt destroy
terragrunt apply
