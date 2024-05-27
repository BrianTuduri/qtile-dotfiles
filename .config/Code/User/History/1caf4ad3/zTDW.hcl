# Define a policy for developers in QA environment
path "secret/data/qa/*" {
    capabilities = ["create", "read", "update", "delete", "list"]
}

# Allow developers to list directories in secret storage to find their accessible secrets in QA
path "secret/metadata/qa/*" {
    capabilities = ["list"]
}

# Allow developers to read common configurations or shared secrets
path "secret/data/common/*" {
    capabilities = ["read"]
}

# Enable developers to renew their own tokens
path "auth/token/renew-self" {
    capabilities = ["update"]
}