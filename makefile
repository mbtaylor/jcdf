
JAVAC = javac
JAVA = java
JAR = jar

JARFILE = cdf.jar

JSRC = \
       AttributeDescriptorRecord.java \
       Buf.java \
       CdfDescriptorRecord.java \
       GlobalDescriptorRecord.java \
       NioBuf.java \
       Pointer.java \
       Record.java \
       RecordPlan.java \

jar: $(JARFILE)

build: $(JARFILE)

clean:
	rm -rf $(JARFILE) tmp

$(JARFILE): $(JSRC)
	rm -rf tmp
	mkdir -p tmp
	$(JAVAC) -Xlint:unchecked -d tmp $(JSRC) \
            && $(JAR) cf $(JARFILE) -C tmp .
	rm -rf tmp

