package com.example.app.screen.recordatorios.components

import com.example.app.R
import com.example.app.models.Feature

enum class PoiCategory(val iconRes: Int) {
    // Education
    UNIVERSITY(R.drawable.ic_university),
    SCHOOL(R.drawable.ic_school),
    COLLEGE(R.drawable.ic_college),

    // Public Places
    POLICE(R.drawable.ic_police),
    HOSPITAL(R.drawable.ic_hospital),
    FIRE_STATION(R.drawable.ic_fire_station),
    COMMUNITY_CENTRE(R.drawable.ic_community_centre),
    POST_OFFICE(R.drawable.ic_post_office),
    COURTHOUSE(R.drawable.ic_courthouse),

    // Healthcare
    CLINIC(R.drawable.ic_clinic),
    VETERINARY(R.drawable.ic_veterinary),
    PHARMACY(R.drawable.ic_pharmacy),

    // Natural
    WATER(R.drawable.ic_water),
    PARK(R.drawable.ic_park),

    // Leisure
    PITCH(R.drawable.ic_pitch),
    PLAYGROUND(R.drawable.ic_playground),
    STADIUM(R.drawable.ic_stadium),
    NIGHTCLUB(R.drawable.ic_nightclub),

    // Accommodation
    HOTEL(R.drawable.ic_hotel),
    MOTEL(R.drawable.ic_motel),
    APARTMENT(R.drawable.ic_apartment),

    // Shops
    SHOP(R.drawable.ic_shop),
    SUPERMARKET(R.drawable.ic_supermarket),
    KIOSK(R.drawable.ic_kiosk),
    CAR(R.drawable.ic_car_dealer),
    STATIONERY(R.drawable.ic_stationery),
    PHONE(R.drawable.ic_phone_shop),
    HARDWARE(R.drawable.ic_hardware),
    GUEST_HOUSE(R.drawable.ic_guest_house),
    MARKETPLACE(R.drawable.ic_marketplace),
    CLOTHES(R.drawable.ic_clothes),
    COMPUTER(R.drawable.ic_computer),
    ELECTRONICS(R.drawable.ic_electronics),
    DEPARTMENT_STORE(R.drawable.ic_department_store),
    MOBILE_PHONE(R.drawable.ic_mobile_phone),
    BAKERY(R.drawable.ic_bakery),
    BOOKS(R.drawable.ic_books),
    OPTICIAN(R.drawable.ic_optician),
    CANDLES(R.drawable.ic_candles),
    ALCOHOL(R.drawable.ic_alcohol),
    TYRES(R.drawable.ic_tyres),
    MALL(R.drawable.ic_mall),

    // Financial
    BANK(R.drawable.ic_bank),
    ATM(R.drawable.ic_atm),

    // Arts & Culture
    PLACE_OF_WORSHIP(R.drawable.ic_place_of_worship),
    MUSEUM(R.drawable.ic_museum),
    THEATER(R.drawable.ic_theater),
    MONUMENT(R.drawable.ic_monument),

    // Restaurants & Food
    RESTAURANT(R.drawable.ic_restaurant),
    CAFE(R.drawable.ic_cafe),
    FAST_FOOD(R.drawable.ic_fast_food),
    BAR(R.drawable.ic_bar),

    // Transport
    PARKING_SPACE(R.drawable.ic_parking),
    FUEL(R.drawable.ic_fuel),

    // Default
    DEFAULT(R.drawable.ic_marker_blue);

    companion object {
        fun fromCategoryName(categoryName: String?): PoiCategory {
            return when (categoryName?.lowercase()) {
                // Education
                "university" -> UNIVERSITY
                "school" -> SCHOOL
                "college" -> COLLEGE

                // Public
                "police" -> POLICE
                "hospital" -> HOSPITAL
                "fire_station" -> FIRE_STATION
                "community_centre" -> COMMUNITY_CENTRE
                "post_office" -> POST_OFFICE
                "courthouse" -> COURTHOUSE

                // Healthcare
                "clinic" -> CLINIC
                "veterinary" -> VETERINARY
                "pharmacy" -> PHARMACY

                // Natural
                "water" -> WATER
                "park" -> PARK

                // Leisure
                "pitch" -> PITCH
                "playground" -> PLAYGROUND
                "stadium" -> STADIUM
                "nightclub" -> NIGHTCLUB

                // Accommodation
                "hotel" -> HOTEL
                "motel" -> MOTEL
                "apartment" -> APARTMENT

                // Shops
                "shop", "general" -> SHOP
                "supermarket" -> SUPERMARKET
                "kiosk" -> KIOSK
                "car" -> CAR
                "stationery" -> STATIONERY
                "phone" -> PHONE
                "hardware" -> HARDWARE
                "guest_house" -> GUEST_HOUSE
                "marketplace" -> MARKETPLACE
                "clothes" -> CLOTHES
                "computer" -> COMPUTER
                "electronics" -> ELECTRONICS
                "department_store" -> DEPARTMENT_STORE
                "mobile_phone" -> MOBILE_PHONE
                "bakery" -> BAKERY
                "books" -> BOOKS
                "optician" -> OPTICIAN
                "candles" -> CANDLES
                "alcohol" -> ALCOHOL
                "tyres" -> TYRES
                "mall" -> MALL

                // Financial
                "bank" -> BANK
                "atm" -> ATM

                // Arts & Culture
                "place_of_worship" -> PLACE_OF_WORSHIP
                "museum" -> MUSEUM
                "theater", "theatre" -> THEATER
                "monument" -> MONUMENT

                // Food & Drinks
                "restaurant" -> RESTAURANT
                "cafe" -> CAFE
                "fast_food" -> FAST_FOOD
                "bar" -> BAR

                // Transport
                "parking_space", "parking" -> PARKING_SPACE
                "fuel" -> FUEL

                else -> DEFAULT
            }
        }
    }
}

fun Feature.getIconResource(): Int {
    val categoryName = properties?.category_ids?.values?.firstOrNull()?.category_name
    return PoiCategory.fromCategoryName(categoryName).iconRes
}