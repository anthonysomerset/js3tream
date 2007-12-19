#!/bin/sh

# This script will backup a given directory to S3

echo "Starting Backup"
date

BACKUP_TYPE=$1
DIR_TO_BACKUP=$2
BUCKET_NAME=$3
DIFF_FILE=$4
TEMP_FILE=`tempfile -d ~/bin -p s3temp- -s .tgz`


if [ "${DIR_TO_BACKUP}" == ""  -o "${BACKUP_TYPE}" == "" -o "${BUCKET_NAME}" == "" ]; then
  echo "Usage: s3backup.sh <type> <dir> <bucket> <diff file>"
  echo "   <type> = Full or Differential backup.  FULL / DIFF / NORMAL"
  echo "   <dir> = The directory to backup"
  echo "   <bucket> = The bucket to backup to"
  echo "   <diff file> = The tar DIFF file to use."
  exit;
fi

if [ "${BACKUP_TYPE}" != "FULL" -a "${BACKUP_TYPE}" != "DIFF" -a "${BACKUP_TYPE}" != "NORMAL" ]; then
  echo " Invalid bacup type [${BACKUP_TYPE}] Must be one of FULL or DIFF or NORMAL"
  exit;
fi

if [ "${BACKUP_TYPE}" == "FULL" -o "${BACKUP_TYPE}" = "DIFF" ]; then
  if [ "${DIFF_FILE}" == "" ]; then
    echo " Invalid diff file name "
    exit;
  fi
fi


# Here is what we are going to do
echo "--------------------"
echo "Backing up directory [${DIR_TO_BACKUP}]"
echo "Backup type is [${BACKUP_TYPE}]"
echo "Bucket Name is [${BUCKET_NAME}]"
echo "DIFF File = [${DIFF_FILE}]"
echo "Temp File = [${TEMP_FILE}]"
echo "--------------------"

# If it's a full backup, then delete the diff file 
if [ "${BACKUP_TYPE}" == "FULL" ]; then
  echo "Performing a full backup, so deleteing diff file [${DIFF_FILE}]"
  echo "--------------------"
  rm -f ${DIFF_FILE}
fi


# Delete the old bucket, if it exists
echo "Deleteing Old Bucket [${BUCKET_NAME}]"
java -jar js3tream.jar -v -K key.txt -d -b ${BUCKET_NAME}
echo "--------------------"


# Create the TGZ
echo "Creating the TGZ"
if [ "${BACKUP_TYPE}" == "NORMAL" ]; then
  tar -C / -czvpf ${TEMP_FILE} ${DIR_TO_BACKUP} 1>&2> ${TEMP_FILE}.log
else
  tar -g ${DIFF_FILE} -C / -czvpf ${TEMP_FILE} ${DIR_TO_BACKUP} 1>&2> ${TEMP_FILE}.log
fi
echo "--------------------"


# send to S3
echo "Sending to S3"
cat ${TEMP_FILE} | java -Xmx128M -jar js3tream.jar --debug -z 25000000 -n -v -K key.txt -i -b ${BUCKET_NAME}
echo "--------------------"


# Create a backup copy of the original backup diff
if [ "${BACKUP_TYPE}" == "FULL" ]; then
  echo "Creating a copy of the first diff file [${DIFF_FILE}] to [${DIFF_FILE}.orig]"
  cp -f ${DIFF_FILE} ${DIFF_FILE}.orig
  echo "--------------------"

fi

# Clean up temp file
rm ${TEMP_FILE}

echo "Backup Finished"
date
