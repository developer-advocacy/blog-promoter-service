#!/usr/bin/env bash
set -e
set -o pipefail

export APP_NAME=blog-promoter-service
export SECRETS=${APP_NAME}-secrets
export SECRETS_FN=$HOME/${SECRETS}
export IMAGE_NAME=gcr.io/${GCLOUD_PROJECT}/${APP_NAME}

docker rmi -f $IMAGE_NAME
cd $ROOT_DIR
./mvnw -U -DskipTests=true spring-javaformat:apply clean  spring-boot:build-image -Dspring-boot.build-image.imageName=$IMAGE_NAME
docker push $IMAGE_NAME

echo $IMAGE_NAME

touch $SECRETS_FN
echo writing to "$SECRETS_FN "
cat <<EOF >${SECRETS_FN}

SPRING_RABBITMQ_HOST=${SPRING_RABBITMQ_HOST}
SPRING_RABBITMQ_PASSWORD=${SPRING_RABBITMQ_PASSWORD}
SPRING_RABBITMQ_PORT=${SPRING_RABBITMQ_PORT}
SPRING_RABBITMQ_USERNAME=${SPRING_RABBITMQ_USERNAME}
SPRING_RABBITMQ_VIRTUAL_HOST=${SPRING_RABBITMQ_VIRTUAL_HOST}

SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL}
SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD}
SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME}

SOCIALHUB_JOSHLONG_CLIENT_KEY=${SOCIALHUB_JOSHLONG_CLIENT_KEY}
SOCIALHUB_JOSHLONG_CLIENT_KEY_SECRET=${SOCIALHUB_JOSHLONG_CLIENT_KEY_SECRET}
EOF
kubectl delete secrets $SECRETS || echo "no secrets to delete."
kubectl create secret generic $SECRETS --from-env-file $SECRETS_FN
kubectl delete -f $ROOT_DIR/deploy/k8s/deployment.yaml || echo "couldn't delete the deployment as there was nothing deployed."
kubectl apply -f $ROOT_DIR/deploy/k8s
echo "finished deployment."
