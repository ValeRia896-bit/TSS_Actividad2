package com.umss.estudiomercado.modelo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

import static com.umss.estudiomercado.modelo.Campo.Tipo.*;
import static com.umss.estudiomercado.modelo.Opciones.*;

/** Respuesta a la "Encuesta a empleadores" de la Carrera de Ingeniería de Sistemas (UMSS). */
@Entity
   @Table(name = "estudio_empleadores")
   @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class Empleador extends RegistroBase {

    @Campo(tipo = CONTACTO, etiqueta = "Correo", claves = {"correo"})
    @Size(max = 200) private String correo;

    @Campo(tipo = TEXTO, etiqueta = "Empresa o institución", claves = {"nombre de la empresa"})
    @Size(max = 300) @Column(length = 300) private String nombreEmpresa;

    @Campo(tipo = CATEGORICO, etiqueta = "Tipo de organización", opciones = {"Público", "Privado"},
            claves = {"tipo de organizacion"})
    @Column(length = 40) private String tipoOrganizacion;

    @Campo(tipo = CATEGORICO, etiqueta = "Tamaño de la organización",
            opciones = {"Grande (100 funcionarios o más)", "Mediana (entre 31 a 99 funcionarios)",
                    "Pequeña (entre 11 a 30 funcionarios)", "Micro (hasta 10 funcionarios)"},
            claves = {"tamano de la organizacion"})
    @Column(length = 80) private String tamanio;

    @Campo(tipo = CATEGORICO, etiqueta = "Rubro o sector",
            opciones = {"Desarrollo de software", "Servicios de tecnologías de información (TI)",
                    "Telecomunicaciones y redes", "Soporte técnico y mantenimiento de sistemas",
                    "Consultoría en sistemas / transformación digital", "Banca y servicios financieros",
                    "Instituciones públicas / gobierno", "Educación e investigación", "Industria y manufactura",
                    "Comercio y comercio electrónico", "Salud", "Seguridad informática / ciberseguridad",
                    "Análisis de datos / inteligencia de negocios", "Startups o emprendimientos tecnológicos", "Otros"},
            claves = {"rubro o sector"})
    @Column(length = 120) private String rubro;

    // ---- EMPLEABILIDAD ----
    @Campo(tipo = BOOLEANO, etiqueta = "Contrató ingenieros de Sistemas (últimos 5 años)",
            claves = {"ha contratado ingenieros"})
    private Boolean contratoUltimos5Anios;

    @Campo(tipo = CATEGORICO, etiqueta = "Posibilidad de contratar a futuro",
            opciones = {"Sí, existe una alta probabilidad de contratación",
                    "Sí, dependiendo del crecimiento o nuevos proyectos de la organización",
                    "No se tiene prevista la contratación actualmente",
                    "No requiere profesionales del área de Sistemas"},
            claves = {"posibilidad de incorporar"})
    @Column(length = 120) private String posibilidadContratar;

    @Campo(tipo = CATEGORICO, etiqueta = "Nivel de formación que demanda",
            opciones = {"Licenciatura", "Diplomado", "Especialidad", "Maestría", "Doctorado", "Post- doctorado"},
            claves = {"nivel de formacion academica"})
    @Column(length = 60) private String nivelFormacionDemandado;

    @Campo(tipo = CATEGORICO, etiqueta = "Medio de convocatoria",
            opciones = {"Medios escritos y audiovisuales", "Internet y Redes sociales",
                    "Postulación competitiva (convocatorias públicas y privadas)", "Oferta profesional personal",
                    "Recomendación de terceros", "Otros"},
            claves = {"a traves de que medio"})
    @Column(length = 120) private String medioConvocatoria;

    @Campo(tipo = MULTIPLE, etiqueta = "Cargos que ocupan los titulados",
            opciones = {"De apoyo / asistencia tecnológica",
                    "Técnico (desarrollo de software, administración de sistemas, redes, bases de datos, soporte TI)",
                    "Analista (sistemas, datos, procesos o soluciones tecnológicas)",
                    "Especialista (ciberseguridad, arquitectura de software, infraestructura tecnológica, entre otros)",
                    "Supervisor / Coordinador de área tecnológica",
                    "Jefatura o Gerencial (gestión de proyectos, dirección de TI, transformación digital)",
                    "Consultor tecnológico"},
            claves = {"que tipo de cargos"})
    @Column(columnDefinition = "TEXT") private String cargos;

    @Campo(tipo = TEXTO, etiqueta = "Áreas y tendencias emergentes", claves = {"nuevas areas de conocimiento"})
    @Column(columnDefinition = "TEXT") private String areasEmergentes;

    @Campo(tipo = TEXTO, etiqueta = "Herramientas tecnológicas requeridas", claves = {"nuevas herramientas tecnologicas"})
    @Column(columnDefinition = "TEXT") private String herramientas;

    @Campo(tipo = TEXTO, etiqueta = "Habilidades y competencias fundamentales",
            claves = {"habilidades competencias y destrezas"})
    @Column(columnDefinition = "TEXT") private String habilidades;

    // ---- FORMACIÓN Y PERFIL PROFESIONAL (criterios de valoración) ----
    @Campo(tipo = CATEGORICO, etiqueta = "La carrera brinda confianza como formadora",
            opciones = {EMP_TA, EMP_PA, EMP_TD, EMP_NS}, claves = {"brinda confianza"})
    @Column(length = 40) private String critConfianza;

    @Campo(tipo = CATEGORICO, etiqueta = "Título consistente con las competencias",
            opciones = {EMP_TA, EMP_PA, EMP_TD, EMP_NS}, claves = {"titulo profesional otorgado"})
    @Column(length = 40) private String critTitulo;

    @Campo(tipo = CATEGORICO, etiqueta = "La carrera consulta nuestra opinión",
            opciones = {EMP_TA, EMP_PA, EMP_TD, EMP_NS}, claves = {"consultan regularmente"})
    @Column(length = 40) private String critConsulta;

    @Campo(tipo = CATEGORICO, etiqueta = "Conoce el perfil de egreso",
            opciones = {EMP_TA, EMP_PA, EMP_TD, EMP_NS}, claves = {"conozco el perfil"})
    @Column(length = 40) private String critConocePerfil;

    @Campo(tipo = CATEGORICO, etiqueta = "Perfil coherente con el desempeño",
            opciones = {EMP_TA, EMP_PA, EMP_TD, EMP_NS}, claves = {"coherente con el desempeno"})
    @Column(length = 40) private String critCoherencia;

    @Campo(tipo = CATEGORICO, etiqueta = "Desempeño destacado profesional e integral",
            opciones = {EMP_TA, EMP_PA, EMP_TD, EMP_NS}, claves = {"desempeno destacado"})
    @Column(length = 40) private String critDesempeno;

    @Campo(tipo = CATEGORICO, etiqueta = "Incorpora necesidades del sector en prácticas y titulación",
            opciones = {EMP_TA, EMP_PA, EMP_TD, EMP_NS}, claves = {"incorpora las necesidades del sector"})
    @Column(length = 40) private String critPracticas;

    @Campo(tipo = CATEGORICO, etiqueta = "Competencias laborales demostradas",
            opciones = {EMP_TA, EMP_PA, EMP_TD, EMP_NS}, claves = {"comunicacion efectiva"})
    @Column(length = 40) private String critCompetencias;

    @Campo(tipo = CATEGORICO, etiqueta = "Valores y ética profesional",
            opciones = {EMP_TA, EMP_PA, EMP_TD, EMP_NS}, claves = {"etica profesional"})
    @Column(length = 40) private String critEtica;

    @Campo(tipo = CATEGORICO, etiqueta = "Participó en procesos de retroalimentación",
            opciones = {EMP_TA, EMP_PA, EMP_TD, EMP_NS}, claves = {"he participado en procesos"})
    @Column(length = 40) private String critParticipacion;
}
