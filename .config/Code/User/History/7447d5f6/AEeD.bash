#!/bin/env bash

set -ex

CLUSTER="qa"
PROJECT_ID="2951"
export \
    TF_USERNAME="rdifede" \
    TF_PASSWORD="glpat-4KHG1M9rxze9k7yz58Va" \
    TF_STATE_ADDRESS="https://gitlab.com/api/v4/projects/${PROJECT_ID}/terraform/state/${CLUSTER}" \
    KUBECONFIG_PATH=/home/brian/Downloads/qa.yaml

terragrunt init -reconfigure --terragrunt-log-level debug --terragrunt-debug
terragrunt destroy --terragrunt-log-level debug --terragrunt-debug
terragrunt apply --terragrunt-log-level debug --terragrunt-debug