"""Tests for storage dashboard endpoint."""
import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_dashboard_requires_auth(client: AsyncClient):
    resp = await client.get("/api/storage/dashboard")
    assert resp.status_code == 401


@pytest.mark.asyncio
async def test_dashboard_empty_for_new_user(client: AsyncClient):
    reg = await client.post(
        "/api/auth/register",
        json={"email": "storage@bbg.dev", "password": "pass1234"},
    )
    token = reg.json()["access_token"]

    resp = await client.get(
        "/api/storage/dashboard",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert resp.status_code == 200
    data = resp.json()
    assert "providers" in data
    assert data["grand_total_bytes"] == 0
    assert data["overall_used_percent"] == 0.0


@pytest.mark.asyncio
async def test_refresh_quotas_requires_auth(client: AsyncClient):
    resp = await client.post("/api/storage/refresh-quotas")
    assert resp.status_code == 401
