
JAVAC = javac
JAVA = java
JAR = jar
JAVADOC = javadoc

JARFILE = cdf.jar
STILJAR = stil.jar

TEST_CDFS = data/*.cdf

JSRC = \
       AttributeDescriptorRecord.java \
       AttributeEntryDescriptorRecord.java \
       BankBuf.java \
       Buf.java \
       Bufs.java \
       CdfDescriptorRecord.java \
       CdfFormatException.java \
       CompressedCdfRecord.java \
       CompressedParametersRecord.java \
       CompressedVariableValuesRecord.java \
       GlobalDescriptorRecord.java \
       Pointer.java \
       Record.java \
       RecordFactory.java \
       RecordPlan.java \
       SimpleNioBuf.java \
       SparsenessParametersRecord.java \
       UnusedInternalRecord.java \
       VariableDescriptorRecord.java \
       VariableIndexRecord.java \
       VariableValuesRecord.java \
       \
       BitExpandInputStream.java \
       CdfContent.java \
       CdfInfo.java \
       CdfReader.java \
       Compression.java \
       DataReader.java \
       DataType.java \
       EpochFormatter.java \
       GlobalAttribute.java \
       NumericEncoding.java \
       RecordMap.java \
       RunLengthInputStream.java \
       Shaper.java \
       Variable.java \
       VariableAttribute.java \
       VdrVariable.java \
       WrapperBuf.java \
       \
       CdfDump.java \
       CdfList.java \
       \
       CdfStarTable.java \
       CdfTableBuilder.java \
       CdfTableProfile.java \
       \
       SameTest.java \
       \
       CefFormatException.java \
       CefReader.java \
       CefTableBuilder.java \
       CefValueType.java \

jar: $(JARFILE)

docs: $(JSRC)
	rm -rf docs
	mkdir docs
	$(JAVADOC) -classpath $(STILJAR) -quiet -d docs $(JSRC)

build: jar docs

test: $(JARFILE) $(TEST_CDFS)
	rm -rf tmp; \
	mkdir tmp; \
	for f in $(TEST_CDFS); \
        do \
           files=`./cdfvar.sh -outdir tmp -report $$f`; \
           cmd="java -ea -classpath $(JARFILE) cdf.test.SameTest $$files"; \
           ./cdfvar.sh -outdir tmp -create $$f && \
           echo $$cmd && \
           $$cmd || \
           break; \
        done && \
        rm -rf tmp

clean:
	rm -rf $(JARFILE) tmp docs

$(JARFILE): $(JSRC) $(STILJAR)
	rm -rf tmp
	mkdir -p tmp
	$(JAVAC) -Xlint:unchecked -classpath $(STILJAR) -d tmp $(JSRC) \
            && $(JAR) cf $(JARFILE) -C tmp .
	rm -rf tmp

$(STILJAR):
	curl -L http://www.starlink.ac.uk/stil/stil.jar >$@

