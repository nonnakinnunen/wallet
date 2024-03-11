#!/bin/bash
set -eo pipefail
ARTIFACT_BUCKET=$(cat bucket-name.txt)
TEMPLATE=template-mvn.yml
read -p "Please provide username for the new IAM user?" username
read -s -p "Please provide password for the new IAM user?" password
mvn package
aws cloudformation package --template-file $TEMPLATE --s3-bucket $ARTIFACT_BUCKET --output-template-file out.yml
aws cloudformation deploy --template-file out.yml --stack-name wallet --parameter-overrides PassParameter=$password UserNameParameter=$username --capabilities CAPABILITY_NAMED_IAM
