#!/bin/bash

# Read properties file
PROPS_FILE="src/main/resources/application-default.yaml"
OUTPUT_FILE="src/main/resources/templateRegistrymodifications.xcu"

echo "Generating registry modifications..."

# More flexible extraction that handles indentation
BLOCK_UNTRUSTED=$(awk '/blockUntrustedRefererLinks:/ {print $2}' "$PROPS_FILE")
echo "blockUntrustedRefererLinks: $BLOCK_UNTRUSTED"


# Start XML file
cat > "$OUTPUT_FILE" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<oor:items xmlns:oor="http://openoffice.org/2001/registry"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
EOF

# Item 1
if [ "$BLOCK_UNTRUSTED" = "true" ]; then
  cat >> "$OUTPUT_FILE" << EOF
  <item oor:path="/org.openoffice.Office.Common/Security/Scripting">
    <prop oor:name="BlockUntrustedRefererLinks" oor:op="fuse">
      <value>true</value>
    </prop>
  </item>
EOF
fi

# Close XML
cat >> "$OUTPUT_FILE" << EOF
</oor:items>
EOF


