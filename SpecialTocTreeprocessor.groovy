// Create a specialized table of contents for learning goals
// This extension will add a TOC for learning goals right after the main TOC

// Use a treeprocessor to insert our learning goals TOC
treeprocessor {
    document ->

    println 'SpecialTocTreeprocessor (Groovy) invoked!'

    // Get document backend and language
    def backend = document.getAttribute("backend")
    def language = document.getAttribute("language")
    println "Document backend: ${backend}, language: ${language}"

    // Determine which learning goal prefix to look for based on language
    def lgPrefix = (language == "DE") ? "LZ" : "LG"
    def sectionTitle = (language == "DE") ? "Lernziele im Überblick" : "Learning Goals Overview"

    // Collect all matching learning goal sections
    def learningGoalSections = []

    // Process document blocks recursively to find learning goal sections
    document.getBlocks().each { block ->
        findLearningGoals(block, learningGoalSections, lgPrefix)
    }

    if (learningGoalSections.isEmpty()) {
        println 'No learning goal sections found.'
        return document
    }

    println "Found ${learningGoalSections.size()} learning goal sections"

    // Sort the learning goals by ID for consistency
    learningGoalSections.sort { a, b -> a.getId() <=> b.getId() }

    // Find the insertion point - after the preamble which contains the TOC
    def blocks = document.getBlocks()
    def insertIndex = 0  // Default to beginning

    // Look for preamble (which contains TOC)
    for (int i = 0; i < blocks.size(); i++) {
        def block = blocks[i]
        if (block.getNodeName() == "preamble") {
            insertIndex = i + 1
            println "Found preamble (which contains TOC), will insert after at index ${insertIndex}"
            break
        }
    }

    // If no preamble found, look for the first section
    if (insertIndex == 0) {
        for (int i = 0; i < blocks.size(); i++) {
            def block = blocks[i]
            if (block.getNodeName() == "section") {
                insertIndex = i
                println "No preamble found, inserting before first section at index ${insertIndex}"
                break
            }
        }
    }

    // If still no insertion point, use the beginning
    if (insertIndex == 0) {
        println "No suitable insertion point found, inserting at beginning"
    }

    if (backend == "html5") {
        // For HTML, use a raw HTML block for best formatting
        def htmlContent = """
<div class="sect1">
<h2 id="learning-goals-overview">${sectionTitle}</h2>
<div class="sectionbody">
<div class="ulist">
<ul>
"""
        learningGoalSections.each { section ->
            def sectionItemTitle = section.getTitle()
            def id = section.getId()
            htmlContent += "<li><p><a href=\"#${id}\">${sectionItemTitle}</a></p></li>\n"
        }

        htmlContent += """
</ul>
</div>
</div>
</div>
"""
        // Create a pass block for direct HTML injection
        def block = createBlock(document, "pass", htmlContent)
        blocks.add(insertIndex, block)
        println "Inserted HTML learning goals TOC at index ${insertIndex}"
    } else {
        // For PDF, create and use a different approach

        // Create a section with the title
        def sectionBlock = createBlock(document, "section")
        sectionBlock.title = sectionTitle
//        sectionBlock.style = "discrete" // Don't include in main TOC
        sectionBlock.level = 1 // Top level section

        // Create an unordered list under that section
        def listBlock = createBlock(document, "ulist")

        // Add each learning goal to the list
        learningGoalSections.each { section ->
            def title = section.getTitle()
            def id = section.getId()

            // Create a list item
            def listItem = createBlock(document, "list_item")
            listitem.source = "<<${id},${title}>>"

//            // For the list item content, we'll use a paragraph with a raw link
//            def paragraph = createBlock(document, "paragraph")
//            paragraph.source = "<<${id},${title}>>"
//
//            // Add paragraph to list item
//            listItem.blocks.add(paragraph)

            // Add list item to list
            listBlock.getBlocks.add(listItem)
        }

        // Add the list to the section
        sectionBlock.getBlocks.add(listBlock)

        // Add the section to the document
        blocks.add(insertIndex, sectionBlock)
        println "Inserted PDF learning goals TOC at index ${insertIndex}"
    }

    return document
}

// Helper method to recursively find learning goal sections
def findLearningGoals(block, learningGoals, prefix) {
    // Only process if block is not null
    if (!block) return

    // Check if this is a section with a learning goal ID
    if (block.getNodeName() == "section") {
        def id = block.getId()
        if (id && id.startsWith(prefix)) {
            learningGoals << block
        }
    }

    // Recursively process child blocks
    if (block.getBlocks()) {
        block.getBlocks().each { childBlock ->
            findLearningGoals(childBlock, learningGoals, prefix)
        }
    }
}
