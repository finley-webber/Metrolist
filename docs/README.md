# Metrolist Documentation

This directory contains detailed documentation for Metrolist features and technical implementation details.

## Available Documentation

### User Guides

- **[InnerTune Backup Import Guide](INNERTUNE_BACKUP_IMPORT.md)**
  - Step-by-step guide for importing backups from InnerTune
  - User interface flow and instructions
  - Technical implementation details
  - Troubleshooting common issues
  - Database migration information
  - Data preservation details

### Technical Documentation

- **[Backup & Restore Flowchart](BACKUP_RESTORE_FLOWCHART.md)**
  - Visual flowcharts of the restore process
  - Error handling flows
  - Backup creation process
  - Data flow diagrams
  - Threading model
  - State transitions
  - Performance considerations

## Quick Links

### For Users
- [How to import InnerTune backup](INNERTUNE_BACKUP_IMPORT.md#step-by-step-import-process)
- [Troubleshooting restore issues](INNERTUNE_BACKUP_IMPORT.md#troubleshooting)
- [What data is preserved](INNERTUNE_BACKUP_IMPORT.md#data-preserved-during-import)

### For Developers
- [Technical implementation](INNERTUNE_BACKUP_IMPORT.md#technical-implementation)
- [Database schema](INNERTUNE_BACKUP_IMPORT.md#database-migration)
- [Code references](INNERTUNE_BACKUP_IMPORT.md#technical-reference)
- [Process flowcharts](BACKUP_RESTORE_FLOWCHART.md)

## Contributing to Documentation

If you'd like to improve or add to this documentation:

1. Ensure your documentation is clear and well-structured
2. Include both user-facing and technical details where appropriate
3. Add code examples and references to source files
4. Update this index when adding new documentation files
5. Follow the existing documentation style and formatting

## Related Resources

- [Main README](../README.md) - Project overview and getting started
- [Source Code](../app/src/main/kotlin/com/metrolist/music/) - Implementation details
- [Issues](https://github.com/mostafaalagamy/Metrolist/issues) - Bug reports and feature requests
