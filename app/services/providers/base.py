from abc import ABC, abstractmethod
from dataclasses import dataclass, field


@dataclass
class QuotaInfo:
    total_bytes: int
    used_bytes: int
    free_bytes: int


@dataclass
class RemoteFile:
    remote_id: str
    remote_path: str
    share_url: str = ""


@dataclass
class ProviderAccount:
    provider: str
    account_email: str
    access_token: str
    refresh_token: str = ""


class BaseCloudProvider(ABC):
    """Abstract base class that every cloud provider adapter must implement."""

    @abstractmethod
    async def create_account(self, email: str, password: str) -> ProviderAccount:
        """Register a new cloud account.  Raises NotImplementedError for OAuth-only providers."""
        ...

    @abstractmethod
    async def upload_file(
        self,
        file_path: str,
        filename: str,
        remote_folder: str,
        credentials: dict,
    ) -> RemoteFile:
        """Upload a local file and return the remote file descriptor."""
        ...

    @abstractmethod
    async def get_quota(self, credentials: dict) -> QuotaInfo:
        """Return quota information for the authenticated account."""
        ...

    @abstractmethod
    async def delete_file(self, remote_id: str, credentials: dict) -> bool:
        """Delete a file by its remote ID.  Returns True on success."""
        ...

    @abstractmethod
    async def get_share_link(self, remote_id: str, credentials: dict) -> str:
        """Return a publicly accessible share URL for the file."""
        ...
