package com.example.charpowers.hidden.model;

// When a MOB uses its power:
// ATTACK  = when it's close to whatever it's fighting
// ESCAPE  = when it drops below half health
// RANDOM  = now and then, when a player is near (funny ones)
// CARRY   = written for players, so the mob does the nearest thing it can with it instead (MobMoves, MobBrain)
// PASSIVE = an always-on power that works on mobs too
public enum MobRule { ATTACK, ESCAPE, RANDOM, CARRY, PASSIVE }
