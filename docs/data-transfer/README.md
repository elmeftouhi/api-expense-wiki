 export# Data Transfer (Backup and Restore)

This feature lets you move workspace data safely between environments.

## Endpoints

- `GET /v1/data-transfer/export`
  - Requires tenant admin access on the selected workspace.
  - Returns a full JSON backup payload.

- `POST /v1/data-transfer/import`
  - Creates a **new workspace** and imports backup data into it.
  - Request body:

```json
{
  "tenantName": "My restored workspace",
  "switchDefaultTenant": true,
  "backup": {
    "metadata": {
      "formatVersion": "1.0"
    }
  }
}
```

## Notes

- Import never overwrites existing workspace data.
- Import remaps internal IDs to keep relationships valid.
- Imported records are attached to the importing user where user IDs are required.
- Backup format version must match `1.0`.

