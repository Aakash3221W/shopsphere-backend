$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080"

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Wait-ForGateway {
    param(
        [int]$TimeoutSeconds = 300
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-RestMethod -Method Get -Uri "$baseUrl/actuator/health"
            if ($response.status -eq "UP") {
                return
            }
        } catch {
            Start-Sleep -Seconds 5
        }
    }

    throw "Gateway did not become healthy within $TimeoutSeconds seconds."
}

function Wait-ForRoute {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [int]$TimeoutSeconds = 180
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            Invoke-WebRequest -Method Get -Uri "$baseUrl$Path" -UseBasicParsing | Out-Null
            return
        } catch {
            $statusCode = $null
            if ($_.Exception.Response) {
                $statusCode = [int]$_.Exception.Response.StatusCode
            }

            if ($statusCode -and $statusCode -ne 503) {
                return
            }

            Start-Sleep -Seconds 5
        }
    }

    throw "Route $Path did not become available within $TimeoutSeconds seconds."
}

function Invoke-Api {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [string]$Token,
        $Body = $null,
        [int]$RetryCount = 24,
        [int]$RetryDelaySeconds = 5
    )

    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }

    $params = @{
        Method = $Method
        Uri = "$baseUrl$Path"
        Headers = $headers
    }

    if ($null -ne $Body) {
        $params["ContentType"] = "application/json"
        $params["Body"] = ($Body | ConvertTo-Json -Depth 10)
    }

    for ($attempt = 1; $attempt -le $RetryCount; $attempt++) {
        try {
            return Invoke-RestMethod @params
        } catch {
            $statusCode = $null
            if ($_.Exception.Response) {
                $statusCode = [int]$_.Exception.Response.StatusCode
            }

            $shouldRetry = ($statusCode -eq 503 -or $statusCode -eq 502)
            if (-not $shouldRetry -or $attempt -eq $RetryCount) {
                throw
            }

            Start-Sleep -Seconds $RetryDelaySeconds
        }
    }
}

$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$adminEmail = "admin.$suffix@shopsphere.local"
$customerEmail = "customer.$suffix@shopsphere.local"
$adminPassword = "Admin123!"
$customerPassword = "Customer123!"
$newCustomerPassword = "Customer456!"

Write-Host "Waiting for gateway health..."
Wait-ForGateway
Wait-ForRoute -Path "/gateway/auth/login"

Write-Host "Creating users..."
$null = Invoke-Api -Method Post -Path "/gateway/auth/signup" -Body @{
    name = "Admin User"
    email = $adminEmail
    password = $adminPassword
    role = "ADMIN"
}

$null = Invoke-Api -Method Post -Path "/gateway/auth/signup" -Body @{
    name = "Customer User"
    email = $customerEmail
    password = $customerPassword
    role = "CUSTOMER"
}

Write-Host "Logging in..."
$adminLogin = Invoke-Api -Method Post -Path "/gateway/auth/login" -Body @{
    email = $adminEmail
    password = $adminPassword
}
$adminToken = $adminLogin.data.accessToken
$adminRefreshToken = $adminLogin.data.refreshToken
Assert-True ($null -ne $adminToken) "Admin token was not returned."

$customerLogin = Invoke-Api -Method Post -Path "/gateway/auth/login" -Body @{
    email = $customerEmail
    password = $customerPassword
}
$customerToken = $customerLogin.data.accessToken
Assert-True ($null -ne $customerToken) "Customer token was not returned."

Write-Host "Testing auth endpoints..."
$refreshedAdmin = Invoke-Api -Method Post -Path "/gateway/auth/refresh?refreshToken=$adminRefreshToken"
Assert-True ($null -ne $refreshedAdmin.data.accessToken) "Refresh endpoint did not return a token."

$me = Invoke-Api -Method Get -Path "/gateway/auth/me" -Token $customerToken
Assert-True ($me.data.email -eq $customerEmail) "Current user endpoint returned the wrong user."

$null = Invoke-Api -Method Put -Path "/gateway/auth/changepassword" -Token $customerToken -Body @{
    oldPassword = $customerPassword
    newPassword = $newCustomerPassword
}

$customerLoginAfterChange = Invoke-Api -Method Post -Path "/gateway/auth/login" -Body @{
    email = $customerEmail
    password = $newCustomerPassword
}
$customerToken = $customerLoginAfterChange.data.accessToken

Write-Host "Testing catalog admin endpoints..."
$category = Invoke-Api -Method Post -Path "/gateway/catalog/admin/categories" -Token $adminToken -Body @{
    name = "Electronics $suffix"
    description = "Initial category"
    imageUrl = "https://example.com/category.png"
}
$categoryId = $category.data.id
Assert-True ($null -ne $categoryId) "Category creation did not return an id."

$null = Invoke-Api -Method Put -Path "/gateway/catalog/admin/categories/$categoryId" -Token $adminToken -Body @{
    name = "Electronics Updated $suffix"
    description = "Updated category"
    imageUrl = "https://example.com/category-updated.png"
}

Write-Host "Testing admin product endpoints..."
$adminProduct = Invoke-Api -Method Post -Path "/gateway/admin/products" -Token $adminToken -Body @{
    name = "Headphones $suffix"
    description = "Noise cancelling"
    price = 149.99
    originalPrice = 199.99
    stockQuantity = 20
    imageUrl = "https://example.com/headphones.png"
    categoryId = $categoryId
    featured = $true
}
$productId = $adminProduct.data.id
Assert-True ($null -ne $productId) "Admin product creation did not return an id."

$null = Invoke-Api -Method Put -Path "/gateway/admin/products/$productId" -Token $adminToken -Body @{
    name = "Headphones Pro $suffix"
    description = "Noise cancelling pro"
    price = 159.99
    originalPrice = 209.99
    stockQuantity = 25
    imageUrl = "https://example.com/headphones-pro.png"
    categoryId = $categoryId
    featured = $true
}

$null = Invoke-Api -Method Put -Path "/gateway/admin/products/$productId/stock?quantity=30" -Token $adminToken

Write-Host "Testing public catalog endpoints..."
$products = Invoke-Api -Method Get -Path "/gateway/catalog/products"
Assert-True ($products.data.totalElements -ge 1) "Products listing was empty."

$product = Invoke-Api -Method Get -Path "/gateway/catalog/products/$productId"
Assert-True ($product.data.id -eq $productId) "Product detail endpoint returned the wrong product."

$featuredProducts = Invoke-Api -Method Get -Path "/gateway/catalog/featured"
Assert-True ($featuredProducts.data.Count -ge 1) "Featured products listing was empty."

$categories = Invoke-Api -Method Get -Path "/gateway/catalog/categories"
Assert-True ($categories.data.Count -ge 1) "Categories listing was empty."

$categoryProducts = Invoke-Api -Method Get -Path "/gateway/catalog/categories/$categoryId/products"
Assert-True ($categoryProducts.data.totalElements -ge 1) "Category products listing was empty."

Write-Host "Testing direct catalog admin product endpoints..."
$directCatalogProduct = Invoke-Api -Method Post -Path "/gateway/catalog/admin/products" -Token $adminToken -Body @{
    name = "Speaker $suffix"
    description = "Portable speaker"
    price = 79.99
    originalPrice = 99.99
    stockQuantity = 12
    imageUrl = "https://example.com/speaker.png"
    categoryId = $categoryId
    featured = $false
}
$directProductId = $directCatalogProduct.data.id
Assert-True ($null -ne $directProductId) "Direct catalog product creation did not return an id."

$null = Invoke-Api -Method Put -Path "/gateway/catalog/admin/products/$directProductId" -Token $adminToken -Body @{
    name = "Speaker Plus $suffix"
    description = "Portable speaker plus"
    price = 89.99
    originalPrice = 109.99
    stockQuantity = 15
    imageUrl = "https://example.com/speaker-plus.png"
    categoryId = $categoryId
    featured = $false
}

$null = Invoke-Api -Method Put -Path "/gateway/catalog/admin/products/$directProductId/stock?quantity=18" -Token $adminToken

Write-Host "Testing cart endpoints..."
Wait-ForRoute -Path "/gateway/orders/cart"
$cart = Invoke-Api -Method Get -Path "/gateway/orders/cart" -Token $customerToken
Assert-True ($cart.success) "Cart fetch failed."

$cart = Invoke-Api -Method Post -Path "/gateway/orders/cart/items" -Token $customerToken -Body @{
    productId = $productId
    quantity = 1
}
$cartItemId = $cart.data.items[0].id
Assert-True ($null -ne $cartItemId) "Cart item id was not returned."

$cart = Invoke-Api -Method Put -Path "/gateway/orders/cart/items/$cartItemId" -Token $customerToken -Body @{
    productId = $productId
    quantity = 2
}
Assert-True ($cart.data.items[0].quantity -eq 2) "Cart item quantity was not updated."

$null = Invoke-Api -Method Delete -Path "/gateway/orders/cart/items/$cartItemId" -Token $customerToken

$cart = Invoke-Api -Method Post -Path "/gateway/orders/cart/items" -Token $customerToken -Body @{
    productId = $productId
    quantity = 1
}

$null = Invoke-Api -Method Delete -Path "/gateway/orders/cart" -Token $customerToken

$null = Invoke-Api -Method Post -Path "/gateway/orders/cart/items" -Token $customerToken -Body @{
    productId = $productId
    quantity = 1
}

Write-Host "Testing first order lifecycle..."
$checkout1 = Invoke-Api -Method Post -Path "/gateway/orders/checkout/start" -Token $customerToken
$order1Id = $checkout1.data.id
Assert-True ($null -ne $order1Id) "First checkout did not return an order id."

$null = Invoke-Api -Method Post -Path "/gateway/orders/checkout/address?orderId=$order1Id&address=123%20Main%20St" -Token $customerToken
$null = Invoke-Api -Method Post -Path "/gateway/orders/checkout/delivery?orderId=$order1Id" -Token $customerToken -Body @{
    shippingAddress = "123 Main St"
    deliveryMethod = "STANDARD"
}
$null = Invoke-Api -Method Post -Path "/gateway/orders/place?orderId=$order1Id" -Token $customerToken -Body @{
    paymentMode = "COD"
    transactionId = "txn-$suffix-1"
}

$myOrders = Invoke-Api -Method Get -Path "/gateway/orders/my" -Token $customerToken
Assert-True ($myOrders.data.Count -ge 1) "My orders endpoint returned no orders."

$order1 = Invoke-Api -Method Get -Path "/gateway/orders/$order1Id" -Token $customerToken
Assert-True ($order1.data.id -eq $order1Id) "Order detail endpoint returned the wrong order."

$null = Invoke-Api -Method Post -Path "/gateway/orders/$order1Id/cancel" -Token $customerToken

Write-Host "Testing second order lifecycle for admin endpoints..."
$null = Invoke-Api -Method Post -Path "/gateway/orders/cart/items" -Token $customerToken -Body @{
    productId = $productId
    quantity = 1
}
$checkout2 = Invoke-Api -Method Post -Path "/gateway/orders/checkout/start" -Token $customerToken
$order2Id = $checkout2.data.id
Assert-True ($null -ne $order2Id) "Second checkout did not return an order id."

$null = Invoke-Api -Method Post -Path "/gateway/orders/checkout/address?orderId=$order2Id&address=456%20Market%20St" -Token $customerToken
$null = Invoke-Api -Method Post -Path "/gateway/orders/checkout/delivery?orderId=$order2Id" -Token $customerToken -Body @{
    shippingAddress = "456 Market St"
    deliveryMethod = "EXPRESS"
}
$null = Invoke-Api -Method Post -Path "/gateway/orders/place?orderId=$order2Id" -Token $customerToken -Body @{
    paymentMode = "CARD"
    transactionId = "txn-$suffix-2"
}

Write-Host "Testing direct order admin endpoints..."
$directAdminOrders = Invoke-Api -Method Get -Path "/gateway/orders/admin/orders" -Token $adminToken
Assert-True ($directAdminOrders.data.totalElements -ge 1) "Direct order admin list was empty."

$directAdminOrder = Invoke-Api -Method Get -Path "/gateway/orders/admin/orders/$order2Id" -Token $adminToken
Assert-True ($directAdminOrder.data.id -eq $order2Id) "Direct order admin detail returned the wrong order."

$null = Invoke-Api -Method Put -Path "/gateway/orders/admin/orders/$order2Id/status?status=PACKED" -Token $adminToken

Write-Host "Testing admin service endpoints..."
Wait-ForRoute -Path "/gateway/admin/dashboard"
$dashboard = Invoke-Api -Method Get -Path "/gateway/admin/dashboard" -Token $adminToken
Assert-True ($dashboard.success) "Admin dashboard endpoint failed."

$adminOrders = Invoke-Api -Method Get -Path "/gateway/admin/orders" -Token $adminToken
Assert-True ($adminOrders.data.totalElements -ge 1) "Admin orders list was empty."

$adminOrder = Invoke-Api -Method Get -Path "/gateway/admin/orders/$order2Id" -Token $adminToken
Assert-True ($adminOrder.data.id -eq $order2Id) "Admin order detail returned the wrong order."

$null = Invoke-Api -Method Put -Path "/gateway/admin/orders/$order2Id/status?status=SHIPPED" -Token $adminToken

$adminUsers = Invoke-Api -Method Get -Path "/gateway/admin/users" -Token $adminToken
Assert-True ($adminUsers.data.totalElements -ge 2) "Admin users listing returned too few users."

Write-Host "Testing direct auth admin users endpoint..."
$directUsers = Invoke-Api -Method Get -Path "/gateway/auth/users" -Token $adminToken
Assert-True ($directUsers.data.totalElements -ge 2) "Direct auth users endpoint returned too few users."

Write-Host "Testing logout and cleanup..."
$null = Invoke-Api -Method Post -Path "/gateway/auth/logout" -Token $customerToken
$null = Invoke-Api -Method Delete -Path "/gateway/catalog/admin/products/$directProductId" -Token $adminToken
$null = Invoke-Api -Method Delete -Path "/gateway/admin/products/$productId" -Token $adminToken

Write-Host "All gateway endpoints exercised successfully."
