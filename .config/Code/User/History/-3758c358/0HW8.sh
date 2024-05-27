git pull
docker build --network=host -t registry.gitlab.geocom.com.uy:5005/uy-com-geocom-scm-utils/ansible-ssh:$1 . && \
docker push registry.gitlab.geocom.com.uy:5005/uy-com-geocom-scm-utils/ansible-ssh:$1
