#!/bin/bash

if [ $# -ne 1 ]; then
  echo "expect two argument. For example ./makeall all"
  exit 1
fi

declare -A binaries

if [ $1 == "all" ]; then
  benchs=("adpcm" "epic" "jpeg" "mesa" "pegwit" "g721" "gsm" "mpeg2")
  #benchs=("adpcm" "epic" "g721" "gsm" "jpeg" "mesa" "mpeg2" "pegwit" "pgp")
#  benchs=("adpcm" "epic" "g721" "jpeg" "mesa" "mpeg2" "pgp")
else
  benchs=("$1")
fi
#apps=("perlbench" "bzip2" "gcc" "mcf" "milc" "namd" "gobmk" "dealII" "soplex" "hmmer" "sjeng" "libquantum" "h264ref" "lbm" "omnetpp" "astar" "Xalan")
#binaries=(["adpcm"]="rawdaudio rawcaudio" ["epic"]="epic unepic" ["g721"]="decode encode" ["gsm"]="toast  untoast" ["jpeg"]="cjpeg djpeg" ["mesa"]="mipmap osdemo texgen" ["mpeg2"]="mpeg2decode  mpeg2encode" ["pgp"]="pgp" ["pegwit"]="pegwit")
binaries=(["adpcm"]="rawdaudio rawcaudio" ["epic"]="epic unepic" ["g721"]="decode encode" ["gsm"]="toast  untoast" ["jpeg"]="cjpeg djpeg" ["mesa"]="texgen" ["mpeg2"]="mpeg2decode  mpeg2encode" ["pgp"]="pgp" ["pegwit"]="pegwit")
benchNum=${#benchs[@]}
HOMEDIR="/home/lqingrui"
MEDIA_DIR_BASE="${HOMEDIR}/Benchmark/mediabench3"
BUILDDIR="run/build_base_amd64-m32-gcc42-nn.0000"

UPLOADPRE='spawn scp \-P2200 '
UPLOADPOST=' ztong@systemg.cs.vt.edu:mediabenchrun\/'
#UPLOADPOST=' lqingrui@systemg.cs.vt.edu:mediabenchrun\/'
SPECHOME='\/home\/lqingrui\/Benchmark\/cpu2006\/benchspec\/CPU2006'
BENCHBUILDDIR='run\/build_base_amd64-m32-gcc42-nn.0000'

COPYDIR='/home/lqingrui/binaries/mediabench/'
CODESIZE_REPORT="/home/lqingrui/Benchmark/mediabench3/codesize.txt"

for i in ${benchs[*]}; do
  pushd ${MEDIA_DIR_BASE}/${i}
  # firstly compile the source code
  if [ ${i} == "mesa" ]; then
    make realclean;
    make linux -j8
  elif [ ${i} == "jpeg" ]; then
    pushd jpeg-6a && make clean && make -j8
    popd
  elif [ ${i} == "gsm" ]; then
    make clean && make -j8
  elif [ ${i} == "mpeg2" ]; then
    make clean && make -j8
  elif [ ${i} == "pgp" ]; then
    pushd rsaref && pushd source
    make clean && make -j8
    popd && popd
    pushd src && make clean && make -j8
    popd
  else
    pushd src && make clean && make -j8
    popd
  fi
  if [ $? -ne 0 ]; then
    echo "${i} cannot make successfully!"
    exit 1 
  fi

  # move any statical data during compilation to our directory
  #mv ~/CPNum.txt /home/lqingrui/cpfreq/mediabench/${i}
  mv ~/AntiNum.txt /home/lqingrui/cpfreq/AntiInfo/${i}

  #for TARGET in ${binaries[${i}][*]}; do
  #  pwd
  #  if [ ${i} == "mesa" ]; then
  #    file demos/${TARGET} | grep "dynamically linked" > /dev/null
  #    if [ $? -ne 0 ]; then
  #      echo "NOT DYNAMICALLY LINKED!!!!!!!!!!!!!!!!!!!!!!!"
  #      exit 1
  #    fi
  #    codesize=$(du -b demos/texgen | xargs echo | cut -d' ' -f1)
  #    printf "%-15s %-10d\n" ${TARGET} ${codesize} >> ${CODESIZE_REPORT}
  #    #echo -e "${TARGET} \c" >> ~/Benchmark/mediabench3/codesize.txt
  #    #du -b demos/texgen | xargs echo | cut -d' ' -f1 >> ~/Benchmark/mediabench3/codesize.txt
  #  elif [ ${i} == "jpeg" ]; then
  #    file jpeg-6a/${TARGET} | grep "dynamically linked" > /dev/null
  #    if [ $? -ne 0 ]; then
  #      echo "NOT DYNAMICALLY LINKED!!!!!!!!!!!!!!!!!!!!!!!"
  #      exit 1
  #    fi
  #    codesize=$(du -b jpeg-6a/${TARGET} | xargs echo | cut -d' ' -f1)
  #    printf "%-15s %-10d\n" ${TARGET} ${codesize} >> ${CODESIZE_REPORT}
  #    #echo -e "${TARGET} \c" >> ~/Benchmark/mediabench3/codesize.txt
  #    #du -b jpeg-6a/${TARGET} | xargs echo | cut -d' ' -f1 >> ~/Benchmark/mediabench3/codesize.txt
  #  elif [ ${i} == "gsm" ]; then
  #    file bin/${TARGET} | grep "dynamically linked" > /dev/null
  #    if [ $? -ne 0 ]; then
  #      echo "NOT DYNAMICALLY LINKED!!!!!!!!!!!!!!!!!!!!!!!"
  #      exit 1
  #    fi
  #    codesize=$(du -b bin/${TARGET} | xargs echo | cut -d' ' -f1)
  #    printf "%-15s %-10d\n" ${TARGET} ${codesize} >> ${CODESIZE_REPORT}
  #    #echo -e "${TARGET} \c" >> ~/Benchmark/mediabench3/codesize.txt
  #    #du -b bin/${TARGET} | xargs echo | cut -d' ' -f1 >> ~/Benchmark/mediabench3/codesize.txt
  #  elif [ ${i} == "mpeg2" ]; then
  #    file bin/${TARGET} | grep "dynamically linked" > /dev/null
  #    if [ $? -ne 0 ]; then
  #      echo "NOT DYNAMICALLY LINKED!!!!!!!!!!!!!!!!!!!!!!!"
  #      exit 1
  #    fi
  #    codesize=$(du -b bin/${TARGET} | xargs echo | cut -d' ' -f1)
  #    printf "%-15s %-10d\n" ${TARGET} ${codesize} >> ${CODESIZE_REPORT}
  #    #echo -e "${TARGET} \c" >> ~/Benchmark/mediabench3/codesize.txt
  #    #du -b bin/${TARGET} | xargs echo | cut -d' ' -f1 >> ~/Benchmark/mediabench3/codesize.txt
  #  else
  #    file src/${TARGET} | grep "dynamically linked" > /dev/null
  #    if [ $? -ne 0 ]; then
  #      echo "NOT DYNAMICALLY LINKED!!!!!!!!!!!!!!!!!!!!!!!"
  #      exit 1
  #    fi
  #    codesize=$(du -b src/${TARGET} | xargs echo | cut -d' ' -f1)
  #    printf "%-15s %-10d\n" ${TARGET} ${codesize} >> ${CODESIZE_REPORT}
  #    #echo -e "${TARGET} \c" >> ~/Benchmark/mediabench3/codesize.txt
  #    #du -b src/${TARGET} | xargs echo | cut -d' ' -f1 >> ~/Benchmark/mediabench3/codesize.txt
  #  fi
  #done
  #secondly, upload it to systemg
  #popd
  popd
done





