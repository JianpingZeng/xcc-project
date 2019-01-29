#!/bin/bash

declare -A binaries

if [ $1 == "all" ]; then
  benchs=("adpcm" "epic" "g721" "gsm" "jpeg" "mesa" "mpeg2" "pegwit" "pgp")
else
  benchs=("$1")
fi
#apps=("perlbench" "bzip2" "gcc" "mcf" "milc" "namd" "gobmk" "dealII" "soplex" "hmmer" "sjeng" "libquantum" "h264ref" "lbm" "omnetpp" "astar" "Xalan")
binaries=(["adpcm"]="rawdaudio rawcaudio" ["epic"]="epic unepic" ["g721"]="decode encode" ["gsm"]="toast  untoast" ["jpeg"]="cjpeg djpeg" ["mesa"]="mipmap osdemo texgen" ["mpeg2"]="mpeg2decode  mpeg2encode" ["pgp"]="pgp" ["pegwit"]="pegwit")
benchNum=${#benchs[@]}
HOMEDIR="/home/lqingrui"
MEDIA_DIR_BASE="${HOMEDIR}/Benchmark/mediabench3"

FULL_RESULT="${MEDIA_DIR_BASE}/result"
PART_RESULT="${MEDIA_DIR_BASE}/result-part"


for i in ${benchs[*]}; do
  if [ ${i} == "mesa" ]; then
       tmpFullSum=0                                                                        
       tmpPartSum=0
       tmpFullCode=0
       tmpPartCode=0
       pushd  ${FULL_RESULT}
       while read line 
       do
         tmp=`echo ${line}|cut -d ' ' -f2`
         tmpFullSum=$(echo "${tmp}+${tmpFullSum}" | bc)
       done < texgen
       if [ $? -ne 0 ]; then
         echo "${i} cannot make successfully!"
         exit 1 
       fi
       
       while read line 
       do
         tmp=`echo ${line}|cut -d ' ' -f1`
         if [ ${tmp} == "texgen" ]; then
           tmpFullCode=`echo ${line}|cut -d ' ' -f2`
         fi
       done < codesize.txt
       if [ $? -ne 0 ]; then
         echo "${i} cannot make successfully!"
         exit 1 
       fi
       popd
   
       pushd  ${PART_RESULT}
       while read line 
       do
         tmp=`echo ${line}|cut -d ' ' -f2`
         tmpPartSum=$(echo "${tmp}+${tmpPartSum}" | bc)
       done < texgen
       if [ $? -ne 0 ]; then
         echo "${i} cannot make successfully!"
         exit 1 
       fi
   
       while read line 
       do
         tmp=`echo ${line}|cut -d ' ' -f1`
         if [ ${tmp} == "texgen" ]; then
           tmpPartCode=`echo ${line}|cut -d ' ' -f2`
         fi
       done < codesize.txt
       if [ $? -ne 0 ]; then
         echo "${i} cannot make successfully!"
         exit 1 
       fi
       popd
       reduction=$(echo "scale=6;(${tmpFullSum}-${tmpPartSum})/${tmpFullSum}*100" | bc)
       reduction=$(echo "scale=2;${reduction}/1" | bc)
       codeReduct=$(echo "scale=6;(${tmpFullCode}-${tmpPartCode})/${tmpFullCode}*100" | bc)
       codeReduct=$(echo "scale=2;${codeReduct}/1" | bc)
       echo -e "texgen           ${tmpFullSum} & ${tmpPartSum} & ${reduction}\% & ${tmpFullCode} & ${tmpPartCode} & ${codeReduct}\\\\\\" >> ~/report.txt


    if [ $? -ne 0 ]; then
      echo "${i} cannot make successfully!"
      exit 1 
    fi
  else
    for TARGET in ${binaries[${i}][*]}; do
  #    echo ${TARGET}
      tmpFullSum=0
      tmpPartSum=0
      tmpFullCode=0
      tmpPartCode=0
      pushd  ${FULL_RESULT}
      while read line 
      do
        tmp=`echo ${line}|cut -d ' ' -f2`
        tmpFullSum=$(echo "${tmp}+${tmpFullSum}" | bc)
      done < ${TARGET}
      if [ $? -ne 0 ]; then
        echo "${i} cannot make successfully!"
        exit 1 
      fi
      
      while read line 
      do
        tmp=`echo ${line}|cut -d ' ' -f1`
        if [ ${tmp} == ${TARGET} ]; then
          tmpFullCode=`echo ${line}|cut -d ' ' -f2`
        fi
      done < codesize.txt
      if [ $? -ne 0 ]; then
        echo "${i} cannot make successfully!"
        exit 1 
      fi
      popd
  
      pushd  ${PART_RESULT}
      while read line 
      do
        tmp=`echo ${line}|cut -d ' ' -f2`
        tmpPartSum=$(echo "${tmp}+${tmpPartSum}" | bc)
      done < ${TARGET}
      if [ $? -ne 0 ]; then
        echo "${i} cannot make successfully!"
        exit 1 
      fi
  
      while read line 
      do
        tmp=`echo ${line}|cut -d ' ' -f1`
        if [ ${tmp} == ${TARGET} ]; then
          tmpPartCode=`echo ${line}|cut -d ' ' -f2`
        fi
      done < codesize.txt
      if [ $? -ne 0 ]; then
        echo "${i} cannot make successfully!"
        exit 1 
      fi
      popd
      reduction=$(echo "scale=6;(${tmpFullSum}-${tmpPartSum})/${tmpFullSum}*100" | bc)
      reduction=$(echo "scale=2;${reduction}/1" | bc)
      codeReduct=$(echo "scale=6;(${tmpFullCode}-${tmpPartCode})/${tmpFullCode}*100" | bc)
      codeReduct=$(echo "scale=2;${codeReduct}/1" | bc)
      echo -e "${TARGET}           ${tmpFullSum} & ${tmpPartSum} & ${reduction}\% & ${tmpFullCode} & ${tmpPartCode} & ${codeReduct}\\\\\\" >>~/report.txt
    done
  fi
  popd
done




