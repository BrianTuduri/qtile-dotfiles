#!/bin/bash

# Setup VAULT_ADDR and VAULT_TOKEN
export VAULT_ADDR="https://vault.geocom.com.uy"
export VAULT_TOKEN="hvs.XvjJ4w92UZiqGUEUFYsBizo5"

# Enable AppRole and create a role:
vault auth enable approle
vault write auth/approle/role/geoswitch token_policies="alkosto-policy-qa"

vault_agent_dir=./vault-agent


# Write out a Role ID and Secret ID
vault read -format=json auth/approle/role/geoswitch/role-id \
  | jq -r '.data.role_id' > ${vault_agent_dir}/geoswitch-role_id
vault write -format=json -f auth/approle/role/geoswitch/secret-id \
  | jq -r '.data.secret_id' > ${vault_agent_dir}/geoswitch-secret_id

# Restart the vault-agent-demo container
docker restart vault-agent-demo