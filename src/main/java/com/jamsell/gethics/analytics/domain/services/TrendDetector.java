package com.jamsell.gethics.analytics.domain.services;

import com.jamsell.gethics.analytics.domain.model.commands.RegisterClassifiedTrendCommand;

import java.util.List;

/**
 * Puerto de deteccion de tendencias anomalas. DEFINICION PENDIENTE DEL NEGOCIO: Trello no define indicadores, ventana
 * temporal, valores normales, formula de anomalia ni umbral, por eso el proyecto NO tiene ninguna implementacion. Cuando
 * se definan, cada detector sera un bean que implemente este puerto; el analisis periodico funciona con cero detectores.
 * <p>
 * Contrato: devuelve las tendencias anomalas detectadas en esta ejecucion, ya clasificadas (tipo, nivel de riesgo de la
 * tendencia, riesgo general del propietario y mensaje de alerta). Lista vacia = indicadores dentro de lo normal. Cada
 * elemento devuelto se registra como una tendencia NUEVA: reconocer que una anomalia ya fue informada en una ejecucion
 * anterior ("mismo patron") es responsabilidad del detector, porque ese concepto tampoco esta definido.
 */
public interface TrendDetector {
    List<RegisterClassifiedTrendCommand> detect();
}
