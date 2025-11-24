package com.drinkit.bottle.core

enum class BottleSize(
    val capacityInLiters: Double,
) {
    // Little formats
    FLACON(0.1),
    PICCOLO(0.2), // Also named Split
    CHOPINE(0.25), // Also named Half-Pint
    HALF_BOTTLE(0.375), // Also named Demi
    POT(0.5), // Also named Pint
    CLAVELIN(0.62),

    // Standard formats
    STANDARD(0.75), // Bouteille in French
    LITRE(1.0),

    // Big formats
    MAGNUM(1.5),
    MARIE_JEANNE(2.25),
    DOUBLE_MAGNUM(3.0), // Bordeaux. Équivaut au Jéroboam (3.0L) en Champagne.
    REHOBOAM(4.5), // Champagne. Équivaut au Jéroboam (4.5L) à Bordeaux.
    IMPERIAL(6.0), // Bordeaux. Équivaut au Mathusalem (6.0L) en Champagne.
    SALMANAZAR(9.0),
    BALTHAZAR(12.0),
    NEBUCHADNEZZAR(15.0), // Nabuchodonosor in French
    MELCHIOR(18.0),

    SOLOMON(20.0),
    SOVEREIGN(25.0), // Souverain in French
    PRIMAT(27.0),
    MELCHIZEDEK(30.0);

    fun inStandardBottles(): Double {
        return capacityInLiters / STANDARD.capacityInLiters
    }
}