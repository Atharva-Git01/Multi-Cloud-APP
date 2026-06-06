"""Tests for auth endpoints (register, login, refresh, me)."""
import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_register_and_login(client: AsyncClient):
    payload = {"email": "test@bbg.dev", "password": "Str0ng!Pass", "display_name": "Tester"}

    # Register
    resp = await client.post("/api/auth/register", json=payload)
    assert resp.status_code == 201
    data = resp.json()
    assert "access_token" in data
    assert "refresh_token" in data

    # Login
    resp2 = await client.post(
        "/api/auth/login", json={"email": "test@bbg.dev", "password": "Str0ng!Pass"}
    )
    assert resp2.status_code == 200
    tokens = resp2.json()
    assert tokens["token_type"] == "bearer"


@pytest.mark.asyncio
async def test_duplicate_register(client: AsyncClient):
    payload = {"email": "dup@bbg.dev", "password": "pass123", "display_name": "Dup"}
    await client.post("/api/auth/register", json=payload)
    resp = await client.post("/api/auth/register", json=payload)
    assert resp.status_code == 409


@pytest.mark.asyncio
async def test_login_wrong_password(client: AsyncClient):
    await client.post(
        "/api/auth/register",
        json={"email": "wrong@bbg.dev", "password": "correct"},
    )
    resp = await client.post(
        "/api/auth/login", json={"email": "wrong@bbg.dev", "password": "incorrect"}
    )
    assert resp.status_code == 401


@pytest.mark.asyncio
async def test_me_requires_auth(client: AsyncClient):
    resp = await client.get("/api/auth/me")
    assert resp.status_code == 401


@pytest.mark.asyncio
async def test_me_with_valid_token(client: AsyncClient):
    reg = await client.post(
        "/api/auth/register",
        json={"email": "me@bbg.dev", "password": "mypassword"},
    )
    token = reg.json()["access_token"]

    resp = await client.get("/api/auth/me", headers={"Authorization": f"Bearer {token}"})
    assert resp.status_code == 200
    assert resp.json()["email"] == "me@bbg.dev"


@pytest.mark.asyncio
async def test_token_refresh(client: AsyncClient):
    reg = await client.post(
        "/api/auth/register",
        json={"email": "refresh@bbg.dev", "password": "rpass123"},
    )
    refresh_token = reg.json()["refresh_token"]

    resp = await client.post("/api/auth/refresh", json={"refresh_token": refresh_token})
    assert resp.status_code == 200
    assert "access_token" in resp.json()
