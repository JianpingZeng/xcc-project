#!/bin/bash

if [ $# -ne 4 ]; then
  echo "Must specific all the parameters!!!!!"
  echo "Usage: ./change_compilation.sh [object] [compilation_method] [idempotent_preservation] [checkpoint] "
  echo "For example: ./change_compilation.sh adpcm static none cp"
  exit 1
fi 

if [ $1 == "all" ]; then
  benchs=("adpcm" "epic" "g721" "gsm" "jpeg" "mesa" "mpeg2" "pegwit" "sha" "susan")
  #benchs=("adpcm" "epic" "g721" "gsm" "jpeg" "mesa" "mpeg2" "pegwit")
  #benchs=("adpcm" "epic" "g721" "gsm" "jpeg" "mesa" "mpeg2" "pegwit" "pgp")
else
  benchs=("$1")
fi
COMPILE_METHOD="$2"
IDEM_PRESERVE="$3"
CHECKPOINT="$4"
#grep -rl "preservation=vcf" | xargs sed -i "s/preservation=vcf/preservation=none/g"

MAKEFILE="Makefile.${COMPILE_METHOD}"
echo "Replacing all the Makefile with ${MAKEFILE}"
HOMEDIR="/home/lqingrui"
MEDIA_DIR_BASE="${HOMEDIR}/Benchmark/mediabench3"

for i in ${benchs[*]}; do
  if [ ${i} == "gsm" ]; then
    pushd ${MEDIA_DIR_BASE}/${i}
      rm Makefile
      if [ $? -ne 0 ]; then
        echo "${i} Cannot remove Makefile!"
        exit 1 
      fi
      cp ${MAKEFILE} Makefile
      if [ $? -ne 0 ]; then
        echo "${i} Cannot cp Makefile!"
        exit 1 
      fi
      # dirty hack: need to change these options LLVM in the future.
      grep -r "idempotence-preservation=${IDEM_PRESERVE}" * > /dev/null
      if [ $? -ne 0 ]; then
        if [ ${IDEM_PRESERVE} == "vcf" ]; then
          grep -rl "idempotence-preservation=none" * | xargs sed -i "s/idempotence-preservation=none/idempotence-preservation=${IDEM_PRESERVE}/g"
        else
          grep -rl "idempotence-preservation=vcf" * | xargs sed -i "s/idempotence-preservation=vcf/idempotence-preservation=${IDEM_PRESERVE}/g"
        fi
      else
        echo "--idempotence-preservation=${IDEM_PRESERVE} exist, no need to change"
      fi

      grep -r "livein-checkpoint=$CHECKPOINT" * > /dev/null
      if [ $? -ne 0 ]; then
        if [ ${CHECKPOINT} == "cp" ]; then
          grep -rl "livein-checkpoint=none" * | xargs sed -i "s/livein-checkpoint=none/livein-checkpoint=${CHECKPOINT}/g"
        else
          grep -rl "livein-checkpoint=cp" * | xargs sed -i "s/livein-checkpoint=cp/livein-checkpoint=${CHECKPOINT}/g"
        fi
      else
        echo "--livein-checkpoint=${CHECKPOINT} exist, no need to change"
      fi
    popd
  elif [ ${i} == "jpeg" ]; then
    pushd ${MEDIA_DIR_BASE}/${i}/jpeg-6a
      rm Makefile
      if [ $? -ne 0 ]; then
        echo "${i} Cannot remove Makefile!"
        exit 1 
      fi
      cp ${MAKEFILE} Makefile
      if [ $? -ne 0 ]; then
        echo "${i} Cannot cp Makefile!"
        exit 1 
      fi
    popd
    pushd ${MEDIA_DIR_BASE}/${i}
      # dirty hack: need to change these options LLVM in the future.
      grep -r "idempotence-preservation=${IDEM_PRESERVE}" * > /dev/null
      if [ $? -ne 0 ]; then
        if [ ${IDEM_PRESERVE} == "vcf" ]; then
          grep -rl "idempotence-preservation=none" * | xargs sed -i "s/idempotence-preservation=none/idempotence-preservation=${IDEM_PRESERVE}/g"
        else
          grep -rl "idempotence-preservation=vcf" * | xargs sed -i "s/idempotence-preservation=vcf/idempotence-preservation=${IDEM_PRESERVE}/g"
        fi
      else
        echo "--idempotence-preservation=${IDEM_PRESERVE} exist, no need to change"
      fi

      grep -r "livein-checkpoint=$CHECKPOINT" * > /dev/null
      if [ $? -ne 0 ]; then
        if [ ${CHECKPOINT} == "cp" ]; then
          grep -rl "livein-checkpoint=none" * | xargs sed -i "s/livein-checkpoint=none/livein-checkpoint=${CHECKPOINT}/g"
        else
          grep -rl "livein-checkpoint=cp" * | xargs sed -i "s/livein-checkpoint=cp/livein-checkpoint=${CHECKPOINT}/g"
        fi
      else
        echo "--livein-checkpoint=${CHECKPOINT} exist, no need to change"
      fi
    popd
  elif [ ${i} == "mesa" ]; then
    pushd ${MEDIA_DIR_BASE}/${i}
      rm Make-config
      if [ $? -ne 0 ]; then
        echo "${i} Cannot remove Make-config!"
        exit 1 
      fi
      cp Make-config.${COMPILE_METHOD} Make-config
      if [ $? -ne 0 ]; then
        echo "${i} Cannot cp Make-config!"
        exit 1 
      fi
      pushd demos
        rm Makefile
        if [ $? -ne 0 ]; then
          echo "${i} Cannot remove Makefile!"
          exit 1 
        fi
        cp ${MAKEFILE} Makefile
        if [ $? -ne 0 ]; then
          echo "${i} Cannot cp Makefile!"
          exit 1 
        fi
      popd
      # dirty hack: need to change these options LLVM in the future.
      grep -r "idempotence-preservation=${IDEM_PRESERVE}" * > /dev/null
      if [ $? -ne 0 ]; then
        if [ ${IDEM_PRESERVE} == "vcf" ]; then
          grep -rl "idempotence-preservation=none" * | xargs sed -i "s/idempotence-preservation=none/idempotence-preservation=${IDEM_PRESERVE}/g"
        else
          grep -rl "idempotence-preservation=vcf" * | xargs sed -i "s/idempotence-preservation=vcf/idempotence-preservation=${IDEM_PRESERVE}/g"
        fi
      else
        echo "--idempotence-preservation=${IDEM_PRESERVE} exist, no need to change"
      fi

      grep -r "livein-checkpoint=$CHECKPOINT" * > /dev/null
      if [ $? -ne 0 ]; then
        if [ ${CHECKPOINT} == "cp" ]; then
          grep -rl "livein-checkpoint=none" * | xargs sed -i "s/livein-checkpoint=none/livein-checkpoint=${CHECKPOINT}/g"
        else
          grep -rl "livein-checkpoint=cp" * | xargs sed -i "s/livein-checkpoint=cp/livein-checkpoint=${CHECKPOINT}/g"
        fi
      else
        echo "--livein-checkpoint=${CHECKPOINT} exist, no need to change"
      fi
    popd
  elif [ ${i} == "mpeg2" ]; then
    pushd ${MEDIA_DIR_BASE}/${i}
      rm Makefile
      if [ $? -ne 0 ]; then
        echo "${i} Cannot remove Makefile!"
        exit 1 
      fi
      cp ${MAKEFILE} Makefile
      if [ $? -ne 0 ]; then
        echo "${i} Cannot cp Makefile!"
        exit 1 
      fi
      pushd ./src/mpeg2dec
        rm Makefile
        if [ $? -ne 0 ]; then
          echo "${i} Cannot remove Makefile!"
          exit 1 
        fi
        cp ${MAKEFILE} Makefile
        if [ $? -ne 0 ]; then
          echo "${i} Cannot cp Makefile!"
          exit 1 
        fi
      popd
      pushd ./src/mpeg2enc
        rm Makefile
        if [ $? -ne 0 ]; then
          echo "${i} Cannot remove Makefile!"
          exit 1 
        fi
        cp ${MAKEFILE} Makefile
        if [ $? -ne 0 ]; then
          echo "${i} Cannot cp Makefile!"
          exit 1 
        fi
      popd
      # dirty hack: need to change these options LLVM in the future.
      grep -r "idempotence-preservation=${IDEM_PRESERVE}" * > /dev/null
      if [ $? -ne 0 ]; then
        if [ ${IDEM_PRESERVE} == "vcf" ]; then
          grep -rl "idempotence-preservation=none" * | xargs sed -i "s/idempotence-preservation=none/idempotence-preservation=${IDEM_PRESERVE}/g"
        else
          grep -rl "idempotence-preservation=vcf" * | xargs sed -i "s/idempotence-preservation=vcf/idempotence-preservation=${IDEM_PRESERVE}/g"
        fi
      else
        echo "--idempotence-preservation=${IDEM_PRESERVE} exist, no need to change"
      fi

      grep -r "livein-checkpoint=$CHECKPOINT" * > /dev/null
      if [ $? -ne 0 ]; then
        if [ ${CHECKPOINT} == "cp" ]; then
          grep -rl "livein-checkpoint=none" * | xargs sed -i "s/livein-checkpoint=none/livein-checkpoint=${CHECKPOINT}/g"
        else
          grep -rl "livein-checkpoint=cp" * | xargs sed -i "s/livein-checkpoint=cp/livein-checkpoint=${CHECKPOINT}/g"
        fi
      else
        echo "--livein-checkpoint=${CHECKPOINT} exist, no need to change"
      fi
    popd
  elif [ ${i} == "pgp" ]; then
    pushd ${MEDIA_DIR_BASE}/${i}/src
      rm Makefile
      if [ $? -ne 0 ]; then
        echo "${i} Cannot remove Makefile!"
        exit 1 
      fi
      cp ${MAKEFILE} Makefile
      if [ $? -ne 0 ]; then
        echo "${i} Cannot cp Makefile!"
        exit 1 
      fi
    popd
    pushd ${MEDIA_DIR_BASE}/${i}/rsaref/source
      rm makefile
      if [ $? -ne 0 ]; then
        echo "${i} Cannot remove Makefile!"
        exit 1 
      fi
      cp makefile.${COMPILE_METHOD} makefile
      if [ $? -ne 0 ]; then
        echo "${i} Cannot cp Makefile!"
        exit 1 
      fi
    popd
    pushd ${MEDIA_DIR_BASE}/${i}
      # dirty hack: need to change these options LLVM in the future.
      grep -r "idempotence-preservation=${IDEM_PRESERVE}" * > /dev/null
      if [ $? -ne 0 ]; then
        if [ ${IDEM_PRESERVE} == "vcf" ]; then
          grep -rl "idempotence-preservation=none" * | xargs sed -i "s/idempotence-preservation=none/idempotence-preservation=${IDEM_PRESERVE}/g"
        else
          grep -rl "idempotence-preservation=vcf" * | xargs sed -i "s/idempotence-preservation=vcf/idempotence-preservation=${IDEM_PRESERVE}/g"
        fi
      else
        echo "--idempotence-preservation=${IDEM_PRESERVE} exist, no need to change"
      fi

      grep -r "livein-checkpoint=${CHECKPOINT}" * > /dev/null
      if [ $? -ne 0 ]; then
        if [ ${CHECKPOINT} == "cp" ]; then
          grep -rl "livein-checkpoint=none" * | xargs sed -i "s/livein-checkpoint=none/livein-checkpoint=${CHECKPOINT}/g"
        else
          grep -rl "livein-checkpoint=cp" * | xargs sed -i "s/livein-checkpoint=cp/livein-checkpoint=${CHECKPOINT}/g"
        fi
      else
        echo "--livein-checkpoint=${CHECKPOINT} exist, no need to change"
      fi
    popd
  elif [ ${i} == "sha" ] || [ ${i} == "susan" ]; then
    pushd ${MEDIA_DIR_BASE}/${i}
      rm Makefile
      if [ $? -ne 0 ]; then
        echo "${i} Cannot remove Makefile!"
        exit 1 
      fi
      cp ${MAKEFILE} Makefile
      if [ $? -ne 0 ]; then
        echo "${i} Cannot cp Makefile!"
        exit 1 
      fi
      # dirty hack: need to change these options LLVM in the future.
      grep -r "idempotence-preservation=${IDEM_PRESERVE}" * > /dev/null
      if [ $? -ne 0 ]; then
        if [ ${IDEM_PRESERVE} == "vcf" ]; then
          grep -rl "idempotence-preservation=none" * | xargs sed -i "s/idempotence-preservation=none/idempotence-preservation=${IDEM_PRESERVE}/g"
        else
          grep -rl "idempotence-preservation=vcf" * | xargs sed -i "s/idempotence-preservation=vcf/idempotence-preservation=${IDEM_PRESERVE}/g"
        fi
      else
        echo "--idempotence-preservation=${IDEM_PRESERVE} exist, no need to change"
      fi

      grep -r "livein-checkpoint=${CHECKPOINT}" * > /dev/null
      if [ $? -ne 0 ]; then
        if [ ${CHECKPOINT} == "cp" ]; then
          grep -rl "livein-checkpoint=none" * | xargs sed -i "s/livein-checkpoint=none/livein-checkpoint=${CHECKPOINT}/g"
        else
          grep -rl "livein-checkpoint=cp" * | xargs sed -i "s/livein-checkpoint=cp/livein-checkpoint=${CHECKPOINT}/g"
        fi
      else
        echo "--livein-checkpoint=${CHECKPOINT} exist, no need to change"
      fi
    popd
  else
    pushd ${MEDIA_DIR_BASE}/${i}/src
      rm Makefile
      if [ $? -ne 0 ]; then
        echo "${i} Cannot remove Makefile!"
        exit 1 
      fi
      cp ${MAKEFILE} Makefile
      if [ $? -ne 0 ]; then
        echo "${i} Cannot cp Makefile!"
        exit 1 
      fi
    popd
    pushd ${MEDIA_DIR_BASE}/${i}
      # dirty hack: need to change these options LLVM in the future.
      grep -r "idempotence-preservation=${IDEM_PRESERVE}" * > /dev/null
      if [ $? -ne 0 ]; then
        if [ ${IDEM_PRESERVE} == "vcf" ]; then
          grep -rl "idempotence-preservation=none" * | xargs sed -i "s/idempotence-preservation=none/idempotence-preservation=${IDEM_PRESERVE}/g"
        else
          grep -rl "idempotence-preservation=vcf" * | xargs sed -i "s/idempotence-preservation=vcf/idempotence-preservation=${IDEM_PRESERVE}/g"
        fi
      else
        echo "--idempotence-preservation=${IDEM_PRESERVE} exist, no need to change"
      fi

      grep -r "livein-checkpoint=${CHECKPOINT}" * > /dev/null
      if [ $? -ne 0 ]; then
        if [ ${CHECKPOINT} == "cp" ]; then
          grep -rl "livein-checkpoint=none" * | xargs sed -i "s/livein-checkpoint=none/livein-checkpoint=${CHECKPOINT}/g"
        else
          grep -rl "livein-checkpoint=cp" * | xargs sed -i "s/livein-checkpoint=cp/livein-checkpoint=${CHECKPOINT}/g"
        fi
      else
        echo "--livein-checkpoint=${CHECKPOINT} exist, no need to change"
      fi
    popd
  fi
done




