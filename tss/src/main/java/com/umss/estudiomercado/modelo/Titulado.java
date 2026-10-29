package com.umss.estudiomercado.modelo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import static com.umss.estudiomercado.modelo.Campo.Tipo.*;
import static com.umss.estudiomercado.modelo.Opciones.*;

/** Respuesta a la "Encuesta de opinión a titulados de Ingeniería en Sistemas". */
@Entity
   @Table(name = "estudio_titulados")
   @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class Titulado extends RegistroBase {

    // ---- 0. DATOS GENERALES ----
    @Campo(tipo = CATEGORICO, etiqueta = "Edad actual", claves = {"edad que tiene"})
    @Column(length = 60) private String edad;

    @Campo(tipo = CATEGORICO, etiqueta = "Género", claves = {"genero"})
    @Column(length = 60) private String genero;

    // ---- Formación complementaria ----
    @Campo(tipo = BOOLEANO, etiqueta = "Realizó formación complementaria",
            claves = {"ha realizado o se encuentra realizando"})
    private Boolean formacionComplementaria;

    @Campo(tipo = CATEGORICO, etiqueta = "Máximo nivel de formación complementaria",
            opciones = {"DIPLOMADO", "ESPECIALIDAD", "MAESTRIA", "DOCTORADO", "POS DOCTORADO"},
            claves = {"formacion complementaria de mayor nivel"})
    @Column(length = 60) private String nivelFormacion;

    @Campo(tipo = CATEGORICO, etiqueta = "Dónde cursó el programa",
            opciones = {EUPG, FCYT, IBNORCA, UNIV_INTER, UNIV_PUB, UNIV_PRIV, OTRO},
            claves = {"donde ha cursado"})
    @Column(length = 120) private String lugarFormacion;

    @Campo(tipo = CATEGORICO, etiqueta = "Fuente de financiamiento",
            opciones = {"FINANCIADO CON RECURSOS PROPIOS", "TOTAL FINANCIAMIENTO POR MEDIO DE UNA BECA",
                    "FINANCIADO PARCIALMENTE POR MEDIO DE UNA BECA"},
            claves = {"fuente de financiamiento"})
    @Column(length = 120) private String fuenteFinanciamiento;

    @Campo(tipo = TEXTO, etiqueta = "Nombre del programa", claves = {"nombre del programa"})
    @Size(max = 300) @Column(length = 300) private String nombrePrograma;

    // ---- Interés en posgrado ----
    @Campo(tipo = BOOLEANO, etiqueta = "Interesado en posgrado", claves = {"interesado en realizar estudios"})
    private Boolean interesPosgrado;

    @Campo(tipo = CATEGORICO, etiqueta = "Área de interés del posgrado",
            opciones = {"IA", "Robótica", "Ingenieria de Software", "Base de Datos", "Redes de Datos y Seguridad"},
            claves = {"en cual de las siguientes areas"})
    @Column(length = 80) private String areaInteres;

    @Campo(tipo = CATEGORICO, etiqueta = "Organización preferida para el posgrado",
            opciones = {EUPG, FCYT, IBNORCA, UNIV_INTER, UNIV_PUB, UNIV_PRIV, OTRO},
            claves = {"en que organizacion educativa"})
    @Column(length = 120) private String organizacionPosgrado;

    @Campo(tipo = LIKERT, etiqueta = "Posgrados FCyT afines a Sistemas",
            niveles = {LIK_TD, LIK_D, LIK_A, LIK_TA},
            claves = {"posgrado de la fcyt", "programas de formacion de posgrado"})
    @Min(1) @Max(4) private Integer opinionPosgradoFcyt;

    // ---- Vida profesional ----
    @Campo(tipo = NUMERICO, etiqueta = "Años de vida profesional", claves = {"anos de vida profesional"})
    @DecimalMin("0") @DecimalMax("60") private Double aniosVidaProfesional;

    @Campo(tipo = NUMERICO, etiqueta = "Años de desempleo", claves = {"desempleado"})
    @DecimalMin("0") @DecimalMax("60") private Double aniosDesempleo;

    @Campo(tipo = CATEGORICO, etiqueta = "Situación laboral actual",
            opciones = {"Actualmente me encuentro trabajando un una organización", "Actualmente NO trabajo",
                    "Actualmente dedico mi tiempo COMPLETO a un emprendimiento propio"},
            claves = {"situacion laboral actual"})
    @Column(length = 120) private String situacionLaboral;

    // ---- Emprendimiento ----
    @Campo(tipo = CATEGORICO, etiqueta = "Origen del emprendimiento",
            opciones = {"Idea propia.", "Idea conjunta con compañeros.", "StartUp",
                    "Spin Off (derivado de un proyecto de la Universidad)", "Otro."},
            claves = {"origen del desarrollo"})
    @Column(length = 120) private String origenEmprendimiento;

    @Campo(tipo = CATEGORICO, etiqueta = "Tipo de entregable del negocio",
            opciones = {"Producto", "Servicio", "Ambos"}, claves = {"tipo de entregable"})
    @Column(length = 40) private String tipoEntregable;

    @Campo(tipo = CATEGORICO, etiqueta = "Financiamiento externo del negocio",
            opciones = {"Si, mediante préstamos bancarios.", "Si, mediante inversores.", "No, financiamiento propio."},
            claves = {"financiamiento externo"})
    @Column(length = 80) private String financiamientoExterno;

    @Campo(tipo = LIKERT, etiqueta = "Satisfacción con el negocio",
            niveles = {"Insatisfecho", "Algo satisfecho", "Satisfecho"},
            claves = {"rendimiento actual de su negocio"})
    @Min(1) @Max(3) private Integer satisfaccionNegocio;

    @Campo(tipo = LIKERT, etiqueta = "Importancia de la formación para el negocio",
            niveles = {"No es importante", "Poco Importante", "Importante", "Muy importante"},
            claves = {"iniciar y gestionar su negocio"})
    @Min(1) @Max(4) private Integer importanciaFormacionNegocio;

    // ---- Empleabilidad ----
    @Campo(tipo = BOOLEANO, etiqueta = "Trabajo actual es su primer empleo", claves = {"es su primer empleo"})
    private Boolean primerEmpleo;

    @Campo(tipo = CATEGORICO, etiqueta = "Tiempo para conseguir el primer trabajo",
            opciones = {"Ya trabajaba antes de titularse", "Menos de 1 mes", "Entre 1 - 4 meses",
                    "Entre 4 - 8 meses", "Entre 8 - 12 meses", "Más de 12 meses"},
            claves = {"conseguir su primer trabajo"})
    @Column(length = 60) private String tiempoPrimerTrabajo;

    @Campo(tipo = CATEGORICO, etiqueta = "Empleos desde que se tituló",
            opciones = {"1", "2", "3", "Más de 3"}, claves = {"en cuantos empleos"})
    @Column(length = 20) private String cantidadEmpleos;

    // ---- Valoración de la carrera ----
    @Campo(tipo = LIKERT, etiqueta = "Satisfacción con la formación recibida",
            niveles = {LIK_TD, LIK_D, LIK_A, LIK_TA}, claves = {"grado de satisfaccion"})
    @Min(1) @Max(4) private Integer satisfaccionFormacion;

    @Campo(tipo = LIKERT, etiqueta = "Concordancia formación - requerimientos laborales",
            niveles = {LIK_TD, LIK_D, LIK_A, LIK_TA}, claves = {"concordancia"})
    @Min(1) @Max(4) private Integer concordanciaFormacion;

    @Campo(tipo = MULTIPLE, etiqueta = "Aspectos de la carrera más útiles",
            opciones = {"La aplicación práctica de conceptos teóricos en proyectos reales.",
                    "La oportunidad de realizar pasantías o prácticas profesionales.",
                    "La distribución de los horarios de clases.",
                    "El desarrollo de habilidades analíticas y de resolución de problemas.",
                    "La colaboración de equipos multidisciplinarios para resolver problemas complejos.",
                    "La enseñanza de herramientas y software específico de la industria."},
            claves = {"bastante utilidad"})
    @Column(columnDefinition = "TEXT") private String aspectosUtiles;

    @Campo(tipo = MULTIPLE, etiqueta = "Aspectos de la carrera a mejorar",
            opciones = {"Mayor integración de la teoría con aplicaciones prácticas en el plan de estudios.",
                    "Mayor relacionamiento del estudiante con las empresas por medio de práctica profesionales.",
                    "La distribución de horarios de clases. (Horario para trabajadores, módulos virtuales, etc)",
                    "Actualización constante del plan de estudios para atender tendencias actuales.",
                    "Incremento de actividades extracurriculares (Investigación, servicio social, etc)",
                    "Ofrecimiento de más opciones de especialización."},
            claves = {"pueden mejorarse"})
    @Column(columnDefinition = "TEXT") private String aspectosMejorar;

    @Campo(tipo = MULTIPLE, etiqueta = "Asignaturas con ventaja competitiva",
            opciones = {"Introducción", "Base de Datos", "Inteligencia Artificial", "Redes de Computadora", "Electivas",
                    "T.I.S.", "Básicas (Física, Calculo, Algebras)", "Planificación de Proyectos y Gestión",
                    "Tecnologia Redes Avanzadas"},
            claves = {"ventaja competitiva"})
    @Column(columnDefinition = "TEXT") private String asignaturasVentaja;

    @Campo(tipo = MULTIPLE, etiqueta = "Asignaturas poco útiles",
            opciones = {"Básicas (Física, Calculo, Algebras)", "Aplic. Interactivas para Televisión Digital",
                    "Telefonía IP", "Web Semánticas", "Graficación por Computadora", "Robótica",
                    "Taller de Programación en Bajo Nivel", "Teoría de Autómatas y Leng. Formales",
                    "Programación Funcional", "Teoría de Grafos", "Contabilidad Básica"},
            claves = {"no le resultaron de mucha utilidad"})
    @Column(columnDefinition = "TEXT") private String asignaturasNoUtiles;

    // ---- Red de contactos ----
    @Campo(tipo = BOOLEANO, etiqueta = "Interés en la red de contactos de la carrera", claves = {"red de contactos"})
    private Boolean interesRed;

    @Campo(tipo = CONTACTO, etiqueta = "Email", claves = {"email de contacto"})
    @Size(max = 200) private String email;

    @Campo(tipo = CONTACTO, etiqueta = "Teléfono", claves = {"numero telefonico"})
    @Size(max = 50) private String telefono;

    @Campo(tipo = CONTACTO, etiqueta = "LinkedIn", claves = {"linkedin"})
    @Size(max = 300) private String linkedin;

    /** Regla de coherencia: no se puede haber estado desempleado más años que los de vida profesional. */
    @AssertTrue(message = "Los años de desempleo no pueden superar los años de vida profesional")
    public boolean isDesempleoCoherente() {
        return aniosVidaProfesional == null || aniosDesempleo == null || aniosDesempleo <= aniosVidaProfesional;
    }
}
