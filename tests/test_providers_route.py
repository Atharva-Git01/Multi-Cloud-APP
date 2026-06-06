"""Tests for the /api/providers routes."""
import pytest
from httpx import AsyncClient


async def _register_and_token(client: AsyncClient, email: str, password: str = "pass1234") -> str:
    resp = await client.post(
        "/api/auth/register", json={"email": email, "password": password}
    )
    return resp.json()["access_token"]


@pytest.mark.asyncio
async def test_list_providers_empty(client: AsyncClient):
    token = await _register_and_token(client, "prov_list@bbg.dev")
    resp = await client.get("/api/providers", headers={"Authorization": f"Bearer {token}"})
    assert resp.status_code == 200
    assert resp.json() == []


@pytest.mark.asyncio
async def test_connect_valid_provider(client: AsyncClient):
    token = await _register_and_token(client, "prov_connect@bbg.dev")
    resp = await client.post(
        "/api/providers/connect",
        params={"provider": "google"},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert resp.status_code == 200
    data = resp.json()
    assert "auth_url" in data


@pytest.mark.asyncio
async def test_connect_invalid_provider(client: AsyncClient):
    token = await _register_and_token(client, "prov_invalid@bbg.dev")
    resp = await client.post(
        "/api/providers/connect",
        params={"provider": "fakecloud"},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert resp.status_code == 400


@pytest.mark.asyncio
async def test_disconnect_nonexistent(client: AsyncClient):
    token = await _register_and_token(client, "prov_disc@bbg.dev")
    resp = await client.delete(
        "/api/providers/google",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert resp.status_code == 404
