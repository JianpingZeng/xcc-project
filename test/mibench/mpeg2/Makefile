all: createBin mpeg2dec mpeg2enc

createBin:
	mkdir -p ./bin
mpeg2enc:
	make -C ./src/mpeg2enc

mpeg2dec:
	make -C ./src/mpeg2dec/ 

clean:
	make clean -C ./src/mpeg2enc; make clean -C ./src/mpeg2dec; rm -rf ./bin
