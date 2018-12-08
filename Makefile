SIMTOOLS_DIR        = ../osmocom-sim-tools

PACKAGE_AID         = 0xA0:0x00:0x00:0x01:0x51:0x41:0x43:0x4C
PACKAGE_NAME        = com.github.cheeriotb.aram.cardlet
PACKAGE_VERSION     = 1.00

APPLET_AID          = 0xA0:0x00:0x00:0x01:0x51:0x41:0x43:0x4C:0x00
APPLET_NAME         = com.github.cheeriotb.aram.cardlet.AramApplet

SOURCES             = ./src/com/github/cheeriotb/aram/cardlet/*.java

BUILD_DIR           = ./build
BUILD_CLASSES_DIR   = $(BUILD_DIR)/classes
BUILD_JAVACARD_DIR  = $(BUILD_DIR)/javacard
JAVACARD_SDK_DIR    ?= $(SIMTOOLS_DIR)/javacard
JAVACARD_EXPORT_DIR ?= $(JAVACARD_SDK_DIR)/api21_export_files

ifdef COMSPEC
CLASSPATH           = $(JAVACARD_SDK_DIR)/lib/api21.jar;$(JAVACARD_SDK_DIR)/lib/sim.jar
else
CLASSPATH           = $(JAVACARD_SDK_DIR)/lib/api21.jar:$(JAVACARD_SDK_DIR)/lib/sim.jar
endif

JFLAGS              = -target 1.1 -source 1.3 -J-Duser.language=en -g -d $(BUILD_CLASSES_DIR) -classpath "$(CLASSPATH)"
JAVA                ?= java
JC                  ?= javac

.SUFFIXES: .java .class
.java.class:
	mkdir -p $(BUILD_CLASSES_DIR)
	mkdir -p $(BUILD_JAVACARD_DIR)

	$(JC) $(JFLAGS) $*.java

	$(JAVA) -jar $(JAVACARD_SDK_DIR)/bin/converter.jar    \
		-d $(BUILD_JAVACARD_DIR)                          \
		-classdir $(BUILD_CLASSES_DIR)                    \
		-exportpath $(JAVACARD_EXPORT_DIR)                \
		-applet $(APPLET_AID) $(APPLET_NAME)              \
		$(PACKAGE_NAME) $(PACKAGE_AID) $(PACKAGE_VERSION)

default: classes

classes: $(SOURCES:.java=.class)

clean:
	$(RM) -rf $(BUILD_DIR)
